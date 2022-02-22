package tpietzsch.n5.revise;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.DefaultBlockWriter;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.RawCompression;
import tpietzsch.n5.rai.N5RandomAccessibleIntervalReader;
import tpietzsch.n5.rest.util.ClassJsonAdapter;
import tpietzsch.n5.rest.util.DataBlockAdapter;

public class N5RestServer
{
	private final int port;
	private final String host;
	private final Undertow undertow;
	private final Gson gson;
	private final PathHandler handler;

	public N5RestServer( final int port, final String host )
	{
		this.port = port;
		this.host = host;
		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter( Class.class, new ClassJsonAdapter() );
		gsonBuilder.registerTypeAdapter( DataType.class, new DataType.JsonAdapter() );
		gsonBuilder.registerTypeHierarchyAdapter( Compression.class, CompressionAdapter.getJsonAdapter() );
		gsonBuilder.registerTypeHierarchyAdapter( DataBlock.class, new DataBlockAdapter() );
		gson = gsonBuilder.create();
		handler = Handlers.path();
		undertow = Undertow.builder()
				.addHttpListener( port, host )
				.setHandler( handler )
				.build();
	}

	public void start()
	{
		undertow.start();
	}

	private AtomicInteger idGenerator = new AtomicInteger();

	public void serve( final String path, final N5Reader n5Reader ) throws IOException
	{
		handler.addPrefixPath( path, createDatasetHandler( n5Reader, "/", gson ) );
	}

	public < T extends NativeType< T > > void serve( final String path, RandomAccessibleInterval< T > img, final int... blockSize ) throws IOException
	{
		final int[] defaultBlockSize = blockSize.length == 0 ? new int[] { 64 } : blockSize;
		final T type = Util.getTypeFromInterval( img );
		final int[] cellDimensions = CellImgFactory.getCellDimensions( defaultBlockSize, img.numDimensions(), type.getEntitiesPerPixel() );
		final N5Reader reader = new N5RandomAccessibleIntervalReader<>( img, type, cellDimensions );
		serve( path, reader );
	}

	private static HttpHandler createDatasetHandler( final N5Reader n5Reader, final String pathName, final Gson gson ) throws IOException
	{
		final Compression compression = new RawCompression();
		final DatasetAttributes da = n5Reader.getDatasetAttributes( pathName );
		final DatasetAttributes attributes = new DatasetAttributes( da.getDimensions(), da.getBlockSize(), da.getDataType(), compression);
		return Handlers.path()
				.addPrefixPath( "/attributes.json",
						exchange -> {
							try
							{
								exchange.getResponseHeaders().put( new HttpString( "Access-Control-Allow-Origin" ), "*" );
								exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, "application/json" );
								exchange.getResponseSender().send( gson.toJson( attributes.asMap() ) );
							}
							catch ( Exception e )
							{
								e.printStackTrace();
								throw e;
							}
						} )
				.addPrefixPath( "/",
						new HttpHandler()
						{
							@Override
							public void handleRequest( final HttpServerExchange exchange ) throws Exception
							{
								if( exchange.isInIoThread() )
								{
									exchange.dispatch(this);
									return;
								}
								try
								{
									final String relativePath = exchange.getRelativePath();
									final long[] gridPosition = Arrays.stream( relativePath.substring( 1 ).split( "/" ) )
											.mapToLong( Long::parseLong )
											.toArray();
									final DataBlock< ? > block = n5Reader.readBlock( pathName, da, gridPosition );
									exchange.getResponseHeaders().put( new HttpString( "Access-Control-Allow-Origin" ), "*" );
									exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, "application/octet-stream" );

									final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
									DefaultBlockWriter.writeBlock( outputStream, attributes, block );
									exchange.getResponseSender().send( ByteBuffer.wrap( outputStream.toByteArray() ) );
								}
								catch ( NumberFormatException e )
								{
									exchange.setStatusCode( 403 );
									exchange.endExchange();
								}
								catch ( Exception e )
								{
									e.printStackTrace();
									throw e;
								}
							}
						} );
	}

}

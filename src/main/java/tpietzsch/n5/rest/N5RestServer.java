package tpietzsch.n5.rest;

import tpietzsch.n5.rai.N5RandomAccessibleIntervalReader;
import tpietzsch.n5.rest.util.ClassJsonAdapter;
import tpietzsch.n5.rest.util.DataBlockAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import java.util.concurrent.atomic.AtomicInteger;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import tpietzsch.n5.rest.util.Parameters;

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

	public N5RestHandle serve( final N5Reader n5Reader )
	{
		final int id = idGenerator.getAndIncrement();
		final String prefix = String.format( "img%d", id );
		handler.addPrefixPath( prefix, createN5Handler( n5Reader, gson ) );
		return new N5RestHandle( String.format( "http://%s:%d/%s", host, port, prefix ) );
	}

	public < T extends NativeType< T > > ImgRestHandle serve( RandomAccessibleInterval< T > img, final int... blockSize )
	{
		final int[] defaultBlockSize = blockSize.length == 0 ? new int[] { 64 } : blockSize;
		final T type = Util.getTypeFromInterval( img );
		final int[] cellDimensions = CellImgFactory.getCellDimensions( defaultBlockSize, img.numDimensions(), type.getEntitiesPerPixel() );
		final N5Reader reader = new N5RandomAccessibleIntervalReader<>( img, type, cellDimensions );
		return new ImgRestHandle( serve( reader ), "/" );
	}

	private static PathHandler createN5Handler( final N5Reader n5Reader, final Gson gson )
	{
		return Handlers.path()
					.addPrefixPath( "/getAttribute",
							exchange -> {
								try
								{
									exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, "application/json" );
									final Parameters params = new Parameters( exchange, gson );
									final String pathName = params.getString( "pathName" );
									final String key = params.getString( "key" );
									final Class< ? > clazz = params.fromJson( "clazz", Class.class );
									final Object result = n5Reader.getAttribute( pathName, key, clazz );
									final String s = gson.toJson( result );
									exchange.getResponseSender().send( s );
								}
								catch ( Exception e )
								{
									e.printStackTrace();
									throw e;
								}
							} )
					.addPrefixPath( "/getDatasetAttributes",
							exchange -> {
								try
								{
									exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, "application/json" );
									final Parameters params = new Parameters( exchange, gson );
									final String pathName = params.getString( "pathName" );
									final Object result = n5Reader.getDatasetAttributes( pathName );
									final String s = gson.toJson( result );
									exchange.getResponseSender().send( s );
								}
								catch ( Exception e )
								{
									e.printStackTrace();
									throw e;
								}
							} )
					.addPrefixPath( "/readBlock",
							exchange -> {
								try
								{
									exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, "application/json" );
									final Parameters params = new Parameters( exchange, gson );
									final String pathName = params.getString( "pathName" );
									final DatasetAttributes datasetAttributes = params.fromJson( "datasetAttributes", DatasetAttributes.class );
									final long[] gridPosition = params.fromJson( "gridPosition", long[].class );
									final Object result = n5Reader.readBlock( pathName, datasetAttributes, gridPosition );
									final String s = gson.toJson( result );
									exchange.getResponseSender().send( s );
								}
								catch ( Exception e )
								{
									e.printStackTrace();
									throw e;
								}
							} )
					.addPrefixPath( "/exists",
							exchange -> {
								try
								{
									exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, "application/json" );
									final Parameters params = new Parameters( exchange, gson );
									final String pathName = params.getString( "pathName" );
									final boolean result = n5Reader.exists( pathName );
									final String s = gson.toJson( result );
									exchange.getResponseSender().send( s );
								}
								catch ( Exception e )
								{
									e.printStackTrace();
									throw e;
								}
							} );
	}

}

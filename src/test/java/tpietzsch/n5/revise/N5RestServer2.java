package tpietzsch.n5.revise;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.DefaultBlockWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.RawCompression;
import tpietzsch.n5.rest.util.ClassJsonAdapter;
import tpietzsch.n5.rest.util.DataBlockAdapter;

public class N5RestServer2
{
	private final int port;
	private final String host;
	private final Gson gson;

	private final HttpServer server;

	public N5RestServer2( final int port, final String host ) throws IOException
	{
		this.port = port;
		this.host = host;
		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter( Class.class, new ClassJsonAdapter() );
		gsonBuilder.registerTypeAdapter( DataType.class, new DataType.JsonAdapter() );
		gsonBuilder.registerTypeHierarchyAdapter( Compression.class, CompressionAdapter.getJsonAdapter() );
		gsonBuilder.registerTypeHierarchyAdapter( DataBlock.class, new DataBlockAdapter() );
		gson = gsonBuilder.create();

		server = HttpServer.create(new InetSocketAddress("localhost", 8080), 1000);
		server.setExecutor( Executors.newFixedThreadPool( 64 ) );
	}

	public void start()
	{
		server.start();
	}

	public void serve( final String path, final N5Reader n5Reader ) throws IOException, URISyntaxException
	{
		final String pathName = "/";
		final Compression compression = new RawCompression();
		final DatasetAttributes da = n5Reader.getDatasetAttributes( pathName );
		final DatasetAttributes attributes = new DatasetAttributes( da.getDimensions(), da.getBlockSize(), da.getDataType(), compression);
		final String contextPath = "/" + path;
		final URI contextURI = new URI( contextPath );
		server.createContext( "/" + path, new HttpHandler()
		{
			@Override
			public void handle( final HttpExchange exchange ) throws IOException
			{
				final URI requestURI = exchange.getRequestURI();
				final String relativePath = contextURI.relativize( requestURI ).getPath();
				if ( "attributes.json".equals( relativePath ) )
				{
					final String response = gson.toJson( attributes.asMap() );
					OutputStream outputStream = exchange.getResponseBody();

					exchange.getResponseHeaders().put( "Access-Control-Allow-Origin", Collections.singletonList( "*" ) );
					exchange.getResponseHeaders().put( "Content-Type", Collections.singletonList( "application/json" ) );
					exchange.sendResponseHeaders(200, response.length());
					exchange.getResponseBody().write(response.getBytes());
					exchange.close();
				}
				else
				{
					try
					{
						System.out.println( "relativePath = " + relativePath );
						final long[] gridPosition = Arrays.stream( relativePath.split( "/" ) )
								.mapToLong( Long::parseLong )
								.toArray();
						final DataBlock< ? > block = n5Reader.readBlock( pathName, da, gridPosition );
						System.out.println( "gridPosition = " + Arrays.toString( gridPosition ) );

						final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
						DefaultBlockWriter.writeBlock( byteStream, attributes, block );
						final byte[] bytes = byteStream.toByteArray();

						try
						{
							Thread.sleep( 10000 );
						}
						catch ( InterruptedException e )
						{
							e.printStackTrace();
						}

						exchange.getResponseHeaders().put( "Access-Control-Allow-Origin", Collections.singletonList( "*" ) );
						exchange.getResponseHeaders().put( "Content-Type", Collections.singletonList( "application/octet-stream" ) );
						exchange.sendResponseHeaders(200, bytes.length );
						exchange.getResponseBody().write(bytes);
						exchange.close();
					}
					catch ( NumberFormatException e )
					{
						exchange.sendResponseHeaders( 403, 0 );
						exchange.close();
					}
				}
			}
		} );
	}



}

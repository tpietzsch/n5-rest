package tpietzsch.n5;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import java.io.IOException;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import tpietzsch.n5.rest.ImgRestHandle;
import tpietzsch.n5.rest.N5RestServer;
import tpietzsch.n5.rest.util.Parameters;

import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;

public class GaussServer
{
	public static void main( String[] args )
	{
		final GaussServer server = new GaussServer( 8080, "localhost", 64 );
		System.out.println( "GaussServer started at '" + server.baseUrl + "'" );
	}

	public String getBaseUrl()
	{
		return baseUrl;
	}

	private final String baseUrl;

	public GaussServer(final int port, final String host, final int... cellDimensions )
	{
		baseUrl = String.format( "http://%s:%d", host, port );

		final N5RestServer imgServer = new N5RestServer( port + 1, host );
		imgServer.start();

		final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory(
				options().cellDimensions( cellDimensions ) );
		final Gson gson = new GsonBuilder().create();
		final Undertow undertow = Undertow.builder()
				.addHttpListener( port, host )
				.setHandler( Handlers.path()
					.addPrefixPath( "/gauss", exchange -> {
						try
						{
							exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, "application/json" );
							final Parameters params = new Parameters( exchange, gson );
							final double[] sigma = params.fromJson( "sigma", double[].class );
							final Img< ? > img = params.fromJson( "source", ImgRestHandle.class ).open();

							final RandomAccessible< ? > source = Views.extendBorder( img );
							final Img result = factory.create(
									Intervals.dimensionsAsLongArray( img ),
									( NativeType ) Util.getTypeFromInterval( img ),
									( CellLoader ) ( cell -> Gauss3.gauss( sigma, ( RandomAccessible ) source, ( SingleCellArrayImg ) cell, 1 ) ) );
							final String resultJson = gson.toJson( imgServer.serve( result, cellDimensions ) );
							exchange.getResponseSender().send( resultJson );
						}
						catch ( Exception e )
						{
							e.printStackTrace();
							throw e;
						}
					} )

				)
				.build();
		undertow.start();
	}
}

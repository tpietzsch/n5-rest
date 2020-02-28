package tpietzsch.n5;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import bdv.util.volatiles.VolatileViews;
import com.google.gson.Gson;
import ij.IJ;
import java.io.IOException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import tpietzsch.n5.rest.ImgRestHandle;
import tpietzsch.n5.rest.N5RestServer;
import tpietzsch.n5.rest.util.Url;

public class GaussClient
{
	public static void main( String[] args ) throws IOException
	{
		final N5RestServer imgServer = new N5RestServer( 8082, "localhost" );
		imgServer.start();

		final String path = "/Users/pietzsch/workspace/data/73.tif";
		final RandomAccessibleInterval< ? > rai = ImageJFunctions.wrapReal( IJ.openImage( path ) );
		final Bdv bdv = BdvFunctions.show( rai, "input" );

		final String server = "http://localhost:8080";
		final Gson gson = new Gson();

		final ImgRestHandle raiHandle = imgServer.serve( ( RandomAccessibleInterval ) rai, 64 );
		final Url url = new Url( server, "gauss", gson )
				.param( "sigma", new double[] { 3, 3, 3 } )
				.param( "source", raiHandle );
		final ImgRestHandle resultHandle = gson.fromJson( url.openReader(), ImgRestHandle.class );

		final Img< ? > smoothed = resultHandle.open();
		final BdvSource smoothedSource = BdvFunctions.show( VolatileViews.wrapAsVolatile( smoothed ), "smoothed", Bdv.options().addTo( bdv ) );
		smoothedSource.setColor( new ARGBType( 0x00ff00 ) );
	}

}

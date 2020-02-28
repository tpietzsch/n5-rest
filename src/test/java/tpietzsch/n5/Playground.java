package tpietzsch.n5;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import java.io.IOException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import tpietzsch.n5.rest.ImgRestHandle;
import tpietzsch.n5.rest.N5RestServer;

public class Playground
{
	public static void main( String[] args ) throws IOException
	{
		final N5FSReader n5Reader = new N5FSReader( "/Users/pietzsch/Desktop/data/n5playground/export.n5/" );
		final RandomAccessibleInterval< UnsignedShortType > img = N5Utils.open( n5Reader, "setup0/timepoint0/s0" );

		final N5RestServer server = new N5RestServer( 8080, "localhost" );
		server.start();
		final ImgRestHandle handle = server.serve( img, 64 );

		final BdvStackSource< ? > source = BdvFunctions.show( handle.open(), "rai" );
		source.setDisplayRange( 0, 255 );
	}
}

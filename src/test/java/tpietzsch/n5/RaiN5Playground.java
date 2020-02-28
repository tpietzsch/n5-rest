package tpietzsch.n5;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import tpietzsch.n5.rai.N5RandomAccessibleIntervalReader;
import java.io.IOException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

public class RaiN5Playground
{
	public static void main( String[] args ) throws IOException
	{
		final N5FSReader n5Reader = new N5FSReader( "/Users/pietzsch/Desktop/data/n5playground/export.n5/" );
		final RandomAccessibleInterval< UnsignedShortType > img = N5Utils.open( n5Reader, "setup0/timepoint0/s0" );
		final N5RandomAccessibleIntervalReader< UnsignedShortType > raiReader = new N5RandomAccessibleIntervalReader<>( img, new UnsignedShortType(), new int[] { 64, 64, 64 } );
		final RandomAccessibleInterval< UnsignedShortType > img2 = N5Utils.open( raiReader, "/" );
		final BdvStackSource< ? > source = BdvFunctions.show( img2, "rai" );
		source.setDisplayRange( 0, 255 );
	}
}

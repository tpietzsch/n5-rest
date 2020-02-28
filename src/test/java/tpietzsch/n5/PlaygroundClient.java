package tpietzsch.n5;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import tpietzsch.n5.rest.N5RestHandle;
import tpietzsch.n5.rest.util.N5RestReader;
import java.io.IOException;
import net.imglib2.cache.img.CachedCellImg;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

public class PlaygroundClient
{

	public static void main( String[] args ) throws IOException
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final N5RestHandle handle = new N5RestHandle( "http://localhost:8080/img0" );
		show( handle );

//		final URL url = new URL( "http://localhost:8080/exists?pathName=" + URLEncoder.encode(pathName, "UTF-8") );
//		final InputStream stream = url.openStream();
//		final StringBuilder sb = new StringBuilder();
//		int next;
//		while ( ( next = stream.read() ) >= 0 )
//		{
//			sb.append( ( char ) next );
//		}
//		System.out.println( "sb.toString() = " + sb.toString() );
//		stream.close();
	}

	public static void show( N5RestHandle handle ) throws IOException
	{
		final CachedCellImg< ?, ? > img = N5Utils.open( handle.createReader(), "setup0/timepoint0/s0" );
		final BdvStackSource< ? > source = BdvFunctions.show( img, "n5" );
		source.setDisplayRange( 0, 255 );
	}
}

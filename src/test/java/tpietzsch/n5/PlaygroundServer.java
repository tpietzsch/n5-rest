package tpietzsch.n5;

import tpietzsch.n5.rest.N5RestHandle;
import tpietzsch.n5.rest.N5RestServer;
import java.io.IOException;
import org.janelia.saalfeldlab.n5.N5FSReader;

public class PlaygroundServer
{
	public static void main( String[] args ) throws IOException
	{
		final N5FSReader n5Reader = new N5FSReader( "/Users/pietzsch/Desktop/data/n5playground/export.n5/" );
		final N5RestServer server = new N5RestServer( 8080, "localhost" );
		server.start();
		final N5RestHandle handle = server.serve( n5Reader );
		PlaygroundClient.show( handle );
	}
}

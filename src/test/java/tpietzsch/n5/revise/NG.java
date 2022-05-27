package tpietzsch.n5.revise;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ij.IJ;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import tpietzsch.n5.rai.N5RandomAccessibleIntervalReader;

public class NG
{
	public static void main( String[] args ) throws IOException, URISyntaxException
	{
//		BasicConfigurator.configure();

		final String path = "/Users/pietzsch/workspace/data/DM_MV_110629_TL0_Ch0_Angle0.tif";
		final UnsignedShortType type = new UnsignedShortType();

		open( path, type );

//		final String path = "/Users/pietzsch/workspace/data/73.tif";
//		final UnsignedByteType type = new UnsignedByteType();
	}

	public static < T extends RealType< T > & NativeType< T > > void open( final String path, final T type ) throws IOException, URISyntaxException
	{
		final RandomAccessibleInterval< T > rai = ImageJFunctions.wrapReal( IJ.openImage( path ) );
		System.out.println( Util.getTypeFromInterval( rai ).getClass() );
		final N5RandomAccessibleIntervalReader< T > raiReader = new N5RandomAccessibleIntervalReader<>( rai, type, new int[] { 64, 64, 64 } );

		final DatasetAttributes datasetAttributes = raiReader.getDatasetAttributes( "/" );
		System.out.println( "datasetAttributes = " + datasetAttributes );

		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter( DataType.class, new DataType.JsonAdapter() );
		gsonBuilder.registerTypeHierarchyAdapter( Compression.class, CompressionAdapter.getJsonAdapter() );
		gsonBuilder.disableHtmlEscaping(); // TODO: I don't know whether this should be enabled or disabled...
		Gson gson = gsonBuilder.create();

		final N5RestServer2 server = new N5RestServer2( 8080, "localhost" );
		server.start();
		server.serve( "dataset", raiReader );
		System.out.println( "server running..." );

//		poll();
	}

	public static void poll() throws IOException
	{
		for (int i = 0; i < 100; ++i )
		{
			new Thread( () -> {
				try
				{
					final URL url = new URL( "http://localhost:8080/dataset/0/0/0" );
					final InputStream s = url.openStream();
					int j = s.read();
					System.out.println( "j = " + j );
					s.close();
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
			} ).start();
		}
	}
}

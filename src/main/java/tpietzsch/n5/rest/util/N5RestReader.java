package tpietzsch.n5.rest.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.Map;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;

public class N5RestReader implements N5Reader
{
	private final String baseUrl;
	private final Gson gson;

	public N5RestReader( final String baseUrl )
	{
		this.baseUrl = baseUrl;
		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter( Class.class, new ClassJsonAdapter() );
		gsonBuilder.registerTypeAdapter( DataType.class, new DataType.JsonAdapter() );
		gsonBuilder.registerTypeHierarchyAdapter( Compression.class, CompressionAdapter.getJsonAdapter() );
		gsonBuilder.registerTypeHierarchyAdapter( DataBlock.class, new DataBlockAdapter() );
		gson = gsonBuilder.create();
	}

	@Override
	public < T > T getAttribute( final String pathName, final String key, final Class< T > clazz ) throws IOException
	{
		try
		{
			Url url = new Url( baseUrl, "getAttribute", gson )
					.param( "pathName", pathName )
					.param( "key", key )
					.param( "clazz", clazz );
			return gson.fromJson( url.openReader(), clazz );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public DatasetAttributes getDatasetAttributes( final String pathName ) throws IOException
	{
		return read( "getDatasetAttributes", pathName, DatasetAttributes.class );
	}

	@Override
	public DataBlock< ? > readBlock( final String pathName, final DatasetAttributes datasetAttributes, final long[] gridPosition ) throws IOException
	{
		try
		{
			Url url = new Url( baseUrl, "readBlock", gson )
					.param( "pathName", pathName )
					.param( "datasetAttributes", datasetAttributes )
					.param( "gridPosition", gridPosition );
			return gson.fromJson( url.openReader(), DataBlock.class );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public boolean exists( final String pathName )
	{
		return read( "exists", pathName, boolean.class );
	}

	private < T > T read( final String endpoint, final String pathName, final Class< T > clazz )
	{
		try
		{
			Url url = new Url( baseUrl, endpoint, gson )
					.param( "pathName", pathName );
			return gson.fromJson( url.openReader(), clazz );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public String[] list( final String pathName ) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Map< String, Class< ? > > listAttributes( final String pathName ) throws IOException
	{
		throw new UnsupportedOperationException();
	}
}

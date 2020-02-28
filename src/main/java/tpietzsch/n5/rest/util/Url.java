package tpietzsch.n5.rest.util;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

public class Url
{
	private final StringBuilder sb = new StringBuilder();
	private boolean first = true;
	private final Gson gson;

	public Url( final String baseUrl, final String endpoint, final Gson gson )
	{
		sb.append( baseUrl ).append( "/" ).append( endpoint );
		this.gson = gson;
	}

	public Url param( String key, String value )
	{
		sb.append( first ? "?" : "&" );
		first = false;
		try
		{
			sb.append( key ).append( "=" ).append( URLEncoder.encode( value, "UTF-8" ) );
		}
		catch ( UnsupportedEncodingException e )
		{}
		return this;
	}

	public Url param( String key, Object value )
	{
		sb.append( first ? "?" : "&" );
		first = false;
		try
		{
			sb.append( key ).append( "=" ).append( URLEncoder.encode( gson.toJson( value ), "UTF-8" ) );
		}
		catch ( UnsupportedEncodingException e )
		{}
		return this;
	}

	public final InputStream openStream() throws IOException
	{
		return new URL( sb.toString() ).openStream();
	}

	public final Reader openReader() throws IOException
	{
		return new InputStreamReader( openStream() );
	}
}

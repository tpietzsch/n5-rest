package tpietzsch.n5.rest.util;

import com.google.gson.Gson;
import io.undertow.server.HttpServerExchange;
import java.util.Deque;
import java.util.Map;

public class Parameters
{
	private final Map< String, Deque< String > > params;
	private final Gson gson;

	public Parameters( final HttpServerExchange exchange, final Gson gson )
	{
		params = exchange.getQueryParameters();
		this.gson = gson;
	}

	public String getString( final String key )
	{
		final Deque< String > param = params.get( key );
		if ( param == null )
			return null;
		return param.getFirst();
	}

	public < T > T fromJson( final String key, final Class< T > clazz )
	{
		final String param = getString( key );
		if ( param == null )
			return null;
		return gson.fromJson( param, clazz );
	}
}

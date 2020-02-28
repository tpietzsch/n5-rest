package tpietzsch.n5.rest.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class ClassJsonAdapter implements JsonDeserializer< Class< ? > >, JsonSerializer< Class< ? > >
{
	@Override
	public Class< ? > deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
	{
		try
		{
			return Class.forName( context.deserialize( json, String.class ) );
		}
		catch ( ClassNotFoundException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	@Override
	public JsonElement serialize( final Class< ? > src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		return context.serialize( src.getName() );
	}
}

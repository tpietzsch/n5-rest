package tpietzsch.n5.rest.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.janelia.saalfeldlab.n5.ByteArrayDataBlock;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DoubleArrayDataBlock;
import org.janelia.saalfeldlab.n5.FloatArrayDataBlock;
import org.janelia.saalfeldlab.n5.IntArrayDataBlock;
import org.janelia.saalfeldlab.n5.LongArrayDataBlock;
import org.janelia.saalfeldlab.n5.ShortArrayDataBlock;

public class DataBlockAdapter implements JsonDeserializer< DataBlock< ? > >, JsonSerializer< DataBlock< ? > >
{
	@Override
	public DataBlock< ? > deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
	{
		final JsonObject jsonObject = json.getAsJsonObject();
		final String type = jsonObject.getAsJsonPrimitive( "type" ).getAsString();
		final int[] size = context.deserialize( jsonObject.get( "size" ), int[].class );
		final long[] gridPosition = context.deserialize( jsonObject.get( "gridPosition" ), long[].class );
		final JsonElement data = jsonObject.get( "data" );
		switch ( type )
		{
		case "b":
			return new ByteArrayDataBlock( size, gridPosition, context.deserialize( data, byte[].class ) );
		case "d":
			return new DoubleArrayDataBlock( size, gridPosition, context.deserialize( data, double[].class ) );
		case "f":
			return new FloatArrayDataBlock( size, gridPosition, context.deserialize( data, float[].class ) );
		case "i":
			return new IntArrayDataBlock( size, gridPosition, context.deserialize( data, int[].class ) );
		case "l":
			return new LongArrayDataBlock( size, gridPosition, context.deserialize( data, long[].class ) );
		case "s":
			return new ShortArrayDataBlock( size, gridPosition, context.deserialize( data, short[].class ) );
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public JsonElement serialize( final DataBlock< ? > src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		final JsonObject json = new JsonObject();
		final String type;
		if ( src instanceof ByteArrayDataBlock )
			type = "b";
		else if ( src instanceof DoubleArrayDataBlock )
			type = "d";
		else if ( src instanceof FloatArrayDataBlock )
			type = "f";
		else if ( src instanceof IntArrayDataBlock )
			type = "i";
		else if ( src instanceof LongArrayDataBlock )
			type = "l";
		else if ( src instanceof ShortArrayDataBlock )
			type = "s";
		else
			throw new IllegalArgumentException();
		json.addProperty( "type", type );
		json.add( "size", context.serialize( src.getSize() ) );
		json.add( "gridPosition", context.serialize( src.getGridPosition() ) );
		json.add( "data", context.serialize( src.getData() ) );
		return json;
	}
}

package tpietzsch.n5.rest;

import tpietzsch.n5.rest.util.N5RestReader;
import org.janelia.saalfeldlab.n5.N5Reader;

public class N5RestHandle
{
	final String url;

	public N5RestHandle( final String url )
	{
		this.url = url;
	}

	public N5Reader createReader()
	{
		return new N5RestReader( url );
	}

	@Override
	public String toString()
	{
		final StringBuffer sb = new StringBuffer( "N5RestHandle(" );
		sb.append( "url='" ).append( url );
		sb.append( "'}" );
		return sb.toString();
	}
}

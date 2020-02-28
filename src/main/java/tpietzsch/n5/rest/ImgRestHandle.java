package tpietzsch.n5.rest;

import java.io.IOException;
import net.imglib2.img.Img;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

public class ImgRestHandle
{
	private final N5RestHandle handle;
	private final String dataset;

	public ImgRestHandle( final N5RestHandle handle, final String dataset )
	{
		this.handle = handle;
		this.dataset = dataset;
	}

	public Img< ? > open() throws IOException
	{
		return N5Utils.openVolatile( handle.createReader(), dataset );
	}

	@Override
	public String toString()
	{
		final StringBuffer sb = new StringBuffer( "ImgRestHandle(" );
		sb.append( "url='" ).append( handle.url );
		sb.append( "', dataset='" ).append( dataset ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}

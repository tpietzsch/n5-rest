package tpietzsch.n5.rai;

import net.imglib2.img.basictypeaccess.array.ByteArray;
import tpietzsch.n5.rai.util.CopyBlock;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.Type;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

public class N5RandomAccessibleIntervalReader< T extends NativeType< T > > implements N5Reader
{
	private final RandomAccessibleInterval< T > img;

	private final T type;

	private final DatasetAttributes datasetAttributes;

	private final CellGrid grid;

	private final BlockCreator< T > blockCreator;

	public N5RandomAccessibleIntervalReader( final RandomAccessibleInterval< T > img, final T type, final int[] blockSize )
	{
		if ( img.numDimensions() != blockSize.length )
			throw new IllegalArgumentException( "Mismatched dimensionality of blockSize and img" );
		if ( !Views.isZeroMin( img ) )
			throw new IllegalArgumentException( "Input img must have origin at 0" );

		this.img = img;
		this.type = type;
		final long[] dimensions = Intervals.dimensionsAsLongArray( img );
		final DataType dataType = N5Utils.dataType( type );
		final Compression compression = new RawCompression();
		datasetAttributes = new DatasetAttributes( dimensions, blockSize, dataType, compression );
		grid = new CellGrid( dimensions, blockSize );
		blockCreator = BlockCreator.forType( type );
	}

	@Override
	public < T > T getAttribute( final String pathName, final String key, final Class< T > clazz ) throws IOException
	{
		System.err.println( "NOT IMPLEMENTED" );
		System.err.println( "RaiReader.getAttribute" );
		System.err.println( "pathName = " + pathName + ", key = " + key + ", clazz = " + clazz );
		return null;
	}

	@Override
	public DatasetAttributes getDatasetAttributes( final String pathName ) throws IOException
	{
		return datasetAttributes;
	}

	@Override
	public DataBlock< ? > readBlock( final String pathName, final DatasetAttributes datasetAttributes, final long[] gridPosition ) throws IOException
	{
		final int n = grid.numDimensions();
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		grid.getCellDimensions( gridPosition, cellMin, cellDims );
		final Block< T > block = blockCreator.create( cellDims, cellMin, gridPosition );
		final RandomAccess< T > in = img.randomAccess( block.img );
		final RandomAccess< T > out = block.img.randomAccess();
		final Class< ? extends Type > kl1 = type.getClass();
		final Class< ? extends RandomAccess > kl2 = in.getClass();
		final CopyBlock< T > copyBlock = CopyBlock.create( n, kl1, kl2 );
		in.setPosition( out );
		copyBlock.copyBlock( in, out, cellDims );
		return block.dataBlock;
	}

	@Override
	public boolean exists( final String pathName )
	{
		System.err.println( "NOT IMPLEMENTED" );
		System.err.println( "RaiReader.exists" );
		System.err.println( "pathName = " + pathName );
		return false;
	}

	@Override
	public String[] list( final String pathName ) throws IOException
	{
		System.err.println( "NOT IMPLEMENTED" );
		System.err.println( "RaiReader.list" );
		System.err.println( "pathName = " + pathName );
		return new String[ 0 ];
	}

	@Override
	public Map< String, Class< ? > > listAttributes( final String pathName ) throws IOException
	{
		System.err.println( "NOT IMPLEMENTED" );
		System.err.println( "RaiReader.listAttributes" );
		System.err.println( "pathName = " + pathName );
		return null;
	}

	// -- Helpers --

	private static class Block< T extends NativeType< T > >
	{
		final DataBlock< ? > dataBlock;

		final SingleCellArrayImg< T, ? > img;

		public Block( final DataBlock< ? > dataBlock, final SingleCellArrayImg< T, ? > img )
		{
			this.dataBlock = dataBlock;
			this.img = img;
		}
	}

	private interface BlockCreator< T extends NativeType< T > >
	{
		Block< T > create( final int[] blockSize, final long[] blockMin, final long[] gridPosition );

		static < T extends NativeType< T >, A extends ArrayDataAccess< A > > BlockCreator< T > forType( final T type )
		{
			final DataType dataType = N5Utils.dataType( type );
			final Function< DataBlock< ? >, A > wrap = createWrap( dataType );
			final NativeTypeFactory< T, A > nativeTypeFactory = Cast.unchecked( type.getNativeTypeFactory() );
			return ( blockSize, blockMin, gridPosition ) -> {
				final DataBlock< ? > dataBlock = dataType.createDataBlock( blockSize, gridPosition, ( int ) Intervals.numElements( blockSize ) );
				final SingleCellArrayImg< T, A > img = new SingleCellArrayImg<>( blockSize, blockMin, wrap.apply( dataBlock ), null );
				img.setLinkedType( nativeTypeFactory.createLinkedType( img ) );
				return new Block<>( dataBlock, img );
			};
		}

		static < A > Function< DataBlock< ? >, A > createWrap( final DataType dataType )
		{
			switch ( dataType )
			{
			case INT8:
			case UINT8:
				return block -> Cast.unchecked( new ByteArray( ( byte[] ) block.getData() ) );
			case INT16:
			case UINT16:
				return block -> Cast.unchecked( new ShortArray( ( short[] ) block.getData() ) );
			case INT32:
			case UINT32:
				return block -> Cast.unchecked( new IntArray( ( int[] ) block.getData() ) );
			case INT64:
			case UINT64:
				return block -> Cast.unchecked( new LongArray( ( long[] ) block.getData() ) );
			case FLOAT32:
				return block -> Cast.unchecked( new FloatArray( ( float[] ) block.getData() ) );
			case FLOAT64:
				return block -> Cast.unchecked( new DoubleArray( ( double[] ) block.getData() ) );
			default:
				throw new IllegalArgumentException( "Type " + dataType.name() + " not supported!" );
			}
		}
	}
}

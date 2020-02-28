package tpietzsch.n5.rai.util;

import java.util.Arrays;
import net.imglib2.RandomAccess;
import net.imglib2.loops.ClassCopyProvider;
import net.imglib2.type.Type;

public interface CopyBlock< T extends Type< T > >
{
	void copyBlock( final RandomAccess< T > in, final RandomAccess< T > out, final int[] dimensions );

	static < T extends Type< T > > CopyBlock< T > create(
			final int numDimensions,
			final Class< ? > pixelTypeClass,
			final Class< ? > inAccessClass )
	{
		return CopyBlockInstances.create( numDimensions, pixelTypeClass, inAccessClass );
	}
}

class CopyBlockInstances
{
	@SuppressWarnings( "rawtypes" )
	private static ClassCopyProvider< CopyBlock > provider;

	@SuppressWarnings( "unchecked" )
	public static < T extends Type< T > > CopyBlock< T > create(
			final int numDimensions,
			final Class< ?  > pixelTypeClass,
			final Class< ? > inAccessClass )
	{
		if ( provider == null )
		{
			synchronized ( CopyBlockInstances.class )
			{
				if ( provider == null )
					provider = new ClassCopyProvider<>( Imp.class, CopyBlock.class, int.class );
			}
		}

		Object key = Arrays.asList( numDimensions, pixelTypeClass, inAccessClass );
		return provider.newInstanceForKey( key, numDimensions );
	}

	public static class Imp< T extends Type< T > > implements CopyBlock< T >
	{
		private final int n;

		public Imp( final int n )
		{
			if ( n < 1 || n > 5 )
				throw new IllegalArgumentException();

			this.n = n;
		}

		@Override
		public void copyBlock(
				final RandomAccess< T > in,
				final RandomAccess< T > out,
				final int[] dimensions )
		{
			if ( n == 5 )
				copyBlock5D( out, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], dimensions[ 3 ], dimensions[ 4 ], in );
			if ( n == 4 )
				copyBlock4D( out, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], dimensions[ 3 ], in );
			else if ( n == 3 )
				copyBlock3D( out, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], in );
			else if ( n == 2 )
				copyBlock2D( out, dimensions[ 0 ], dimensions[ 1 ], in );
			else
				copyBlock1D( out, dimensions[ 0 ], in );
		}

		private void copyBlock5D(
				final RandomAccess< T > out,
				final int s0, // size of output image
				final int s1,
				final int s2,
				final int s3,
				final int s4,
				final RandomAccess< T > in )
		{
			for ( int x4 = 0; x4 < s4; ++x4 )
			{
				copyBlock4D( out, s0, s1, s2, s3, in );
				out.fwd( 4 );
				in.fwd( 4 );
			}
			out.move( -s4, 4 );
			in.move( -s4, 4 );
		}

		private void copyBlock4D(
				final RandomAccess< T > out,
				final int s0, // size of output image
				final int s1,
				final int s2,
				final int s3,
				final RandomAccess< T > in )
		{
			for ( int x3 = 0; x3 < s3; ++x3 )
			{
				copyBlock3D( out, s0, s1, s2, in );
				out.fwd( 3 );
				in.fwd( 3 );
			}
			out.move( -s3, 3 );
			in.move( -s3, 3 );
		}

		private void copyBlock3D(
				final RandomAccess< T > out,
				final int s0, // size of output image
				final int s1,
				final int s2,
				final RandomAccess< T > in )
		{
			for ( int x2 = 0; x2 < s2; ++x2 )
			{
				copyBlock2D( out, s0, s1, in );
				out.fwd( 2 );
				in.fwd( 2 );
			}
			out.move( -s2, 2 );
			in.move( -s2, 2 );
		}

		private void copyBlock2D(
				final RandomAccess< T > out,
				final int s0, // size of output image
				final int s1,
				final RandomAccess< T > in )
		{
			for ( int x1 = 0; x1 < s1; ++x1 )
			{
				copyBlock1D( out, s0, in );
				out.fwd( 1 );
				in.fwd( 1 );
			}
			out.move( -s1, 1 );
			in.move( -s1, 1 );
		}

		private void copyBlock1D(
				final RandomAccess< T > out,
				final int s0, // size of output image
				final RandomAccess< T > in )
		{
			for ( int x0 = 0; x0 < s0; ++x0 )
			{
				out.get().set( in.get() );
				out.fwd( 0 );
				in.fwd( 0 );
			}
			out.move( -s0, 0 );
			in.move( -s0, 0 );
		}
	}
}

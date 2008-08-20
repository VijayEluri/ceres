package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;
import com.bc.ceres.binio.smos.SmosProduct;
import com.bc.ceres.binio.util.ByteArrayIOHandler;
import com.bc.ceres.binio.util.ImageIOHandler;
import com.bc.ceres.binio.util.SequenceElementCountResolver;
import static com.bc.ceres.binio.util.TypeBuilder.*;
import junit.framework.TestCase;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;


public class InstanceTest extends TestCase {

    public void testGeneratedInstanceTypes() throws IOException {
        final byte[] byteData = SmosProduct.createTestProductData(SmosProduct.MIR_SCLF1C_FORMAT.getByteOrder());
        final TracingIOHandler ioHandler = new TracingIOHandler(new ByteArrayIOHandler(byteData));
        final IOContext context = new IOContext(SmosProduct.MIR_SCLF1C_FORMAT, ioHandler);

        final CompoundData mirSclf1cData = context.getData();
        final SequenceData snapshotList = mirSclf1cData.getSequence("Snapshot_List");
        final CompoundData snapshotData = snapshotList.getCompound(0);
        final SequenceData gridPointList = mirSclf1cData.getSequence("Grid_Point_List");
        final CompoundData gridPointData = gridPointList.getCompound(0);
        final SequenceData btDataList = gridPointData.getSequence("Bt_Data_List");
        final CompoundData btData = btDataList.getCompound(0);

        assertSame(VarCompound.class, mirSclf1cData.getClass());
        assertSame(FixSequenceOfFixCollections.class, snapshotList.getClass());
        assertSame(FixCompound.class, snapshotData.getClass());
        assertSame(FixSequenceOfVarCollections.class, gridPointList.getClass());
        assertSame(VarCompound.class, gridPointData.getClass());
        assertSame(FixSequenceOfFixCollections.class, btDataList.getClass());
        assertSame(FixCompound.class, btData.getClass());
    }


    public void testFixSequenceOfSimples() throws IOException {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        ios.writeInt(2134);
        ios.writeInt(45);
        ios.writeInt(36134);
        ios.close();

        final ImageInputStream iis = new MemoryCacheImageInputStream(new ByteArrayInputStream(baos.toByteArray()));
        final IOContext context = new IOContext(new Format(new CompoundType("UNDEFINED", new CompoundType.Member[0]), ByteOrder.LITTLE_ENDIAN), new ImageIOHandler(iis));

        SequenceType type = new SequenceType(SimpleType.INT, 3);
        final FixSequenceOfSimples sequenceInstance = new FixSequenceOfSimples(context, null, type, 0);

        assertEquals(3, sequenceInstance.getElementCount());
        assertEquals(3 * 4, sequenceInstance.getSize());
        assertEquals(false, sequenceInstance.isDataAccessible());

        sequenceInstance.makeDataAccessible();

        assertEquals(true, sequenceInstance.isDataAccessible());
        assertEquals(2134, sequenceInstance.getInt(0));
        assertEquals(45, sequenceInstance.getInt(1));
        assertEquals(36134, sequenceInstance.getInt(2));
    }

    public void testFixCompound() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        ios.writeInt(33);
        ios.writeInt(55);
        ios.writeFloat(27.88f);
        ios.close();

        CompoundType type = new CompoundType("compoundTestType", new CompoundType.Member[]{
                new CompoundType.Member("a", SimpleType.UINT),
                new CompoundType.Member("b", SimpleType.FLOAT)
        });
        assertFalse(FixCompound.isCompoundTypeWithinSizeLimit(type, 7));
        assertTrue(FixCompound.isCompoundTypeWithinSizeLimit(type, 8));
        assertTrue(FixCompound.isCompoundTypeWithinSizeLimit(type, 9));

        final byte[] byteData = baos.toByteArray();
        assertEquals(3 * 4, byteData.length);
        final IOContext context = new IOContext(new Format(type, ByteOrder.LITTLE_ENDIAN), new ByteArrayIOHandler(byteData));

        CompoundInstance compoundInstance = InstanceFactory.createCompound(context, null, type, 4, ByteOrder.LITTLE_ENDIAN);
        assertSame(FixCompound.class, compoundInstance.getClass());

        assertEquals(2, compoundInstance.getElementCount());
        assertEquals(2 * 4, compoundInstance.getSize());
        assertEquals(4, compoundInstance.getPosition());
        assertEquals(type, compoundInstance.getType());
        assertEquals(true, compoundInstance.isSizeResolved());
        assertEquals(55, compoundInstance.getInt(0));
        assertEquals(27.88f, compoundInstance.getFloat(1), 0.00001f);
    }

    public void testVarCompound() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(ByteOrder.BIG_ENDIAN);
        ios.writeInt(3);
        ios.writeDouble(111.1);
        ios.writeDouble(222.2);
        ios.writeDouble(333.3);
        ios.close();

        CompoundType type = new CompoundType("compoundTestType",
                                             new CompoundType.Member[]{
                                                     new CompoundType.Member("count", SimpleType.INT),
                                                     new CompoundType.Member("list", new SequenceType(SimpleType.DOUBLE))
                                             });

        Format format = new Format(type, ByteOrder.BIG_ENDIAN);
        format.addSequenceElementCountResolver(type, "list", "count");
        assertFalse(FixCompound.isCompoundTypeWithinSizeLimit(type, 4));
        assertFalse(FixCompound.isCompoundTypeWithinSizeLimit(type, 10));
        assertFalse(FixCompound.isCompoundTypeWithinSizeLimit(type, 10000));

        final byte[] byteData = baos.toByteArray();
        assertEquals(4 + 3 * 8, byteData.length);
        final IOContext context = new IOContext(format, new ByteArrayIOHandler(byteData));

        CompoundData compoundData = context.getData();
        assertTrue(compoundData instanceof CompoundInstance);
        CompoundInstance compoundInstance = (CompoundInstance) compoundData;
        assertSame(VarCompound.class, compoundInstance.getClass());

        assertEquals(2, compoundInstance.getMemberCount());
        assertFalse(compoundInstance.isSizeResolved());

        SequenceData sequenceData = compoundInstance.getSequence(1);
        assertSame(FixSequenceOfSimples.class, sequenceData.getClass());

        compoundInstance.resolveSize();

        assertTrue(compoundInstance.isSizeResolved());
        assertNotSame(type, compoundInstance.getType());

        assertNotNull(sequenceData);
        assertEquals(3, sequenceData.getElementCount());
        assertEquals(111.1, sequenceData.getDouble(0), 1e-10);
        assertEquals(222.2, sequenceData.getDouble(1), 1e-10);
        assertEquals(333.3, sequenceData.getDouble(2), 1e-10);
    }

    public void testFixSequenceOfFixCollections() throws IOException {

        final int n = 11;
        final CompoundType type =
                COMP("U",
                     MEMBER("A", INT),
                     MEMBER("B",
                            SEQ(
                                    COMP("P",
                                         MEMBER("X", DOUBLE),
                                         MEMBER("Y", DOUBLE)),
                                    n
                            )
                     ),
                     MEMBER("C", INT)
                );

        assertTrue(type.isSizeKnown());
        assertEquals(4 + n * (8 + 8) + 4, type.getSize());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(type.getSize());
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.writeInt(12345678);
        for (int i = 0; i < n; i++) {
            ios.writeDouble(20.0 + 0.1 * i);
            ios.writeDouble(40.0 + 0.1 * i);
        }
        ios.writeInt(87654321);
        ios.close();

        final byte[] byteData = baos.toByteArray();
        assertEquals(4 + n * (8 + 8) + 4, byteData.length);
        final IOContext context = new IOContext(new Format(type), new ByteArrayIOHandler(byteData));
        final CompoundData compoundData = context.getData();

        assertSame(FixCompound.class, compoundData.getClass());
        assertSame(FixSequenceOfFixCollections.class, compoundData.getSequence(1).getClass());
        assertSame(FixCompound.class, compoundData.getSequence(1).getCompound(0).getClass());

        assertEquals(12345678, compoundData.getInt(0));
        for (int i = 0; i < n; i++) {
            assertEquals("i=" + i, 20.0 + 0.1 * i, compoundData.getSequence(1).getCompound(i).getDouble(0), 1e-10);
            assertEquals("i=" + i, 40.0 + 0.1 * i, compoundData.getSequence(1).getCompound(i).getDouble(1), 1e-10);
        }
        assertEquals(87654321, compoundData.getInt(2));
    }

    public void testFixSequenceOfVarCollections() throws IOException {

        final int ni = 2;
        final int nj = 3;
        final CompoundType pointType = COMP("Point", MEMBER("X", DOUBLE), MEMBER("Y", DOUBLE));
        final SequenceType seqType1 = SEQ(pointType);
        final SequenceType seqType2 = SEQ(seqType1);
        final CompoundType type = COMP("C", MEMBER("M", seqType2));
        final Format format = new Format(type, ByteOrder.BIG_ENDIAN);
        format.addSequenceTypeMapper(seqType1, new SequenceElementCountResolver() {
            @Override
            public int getElementCount(CollectionData parent, SequenceType sequenceType) throws IOException {
                return ni;
            }
        });
        format.addSequenceTypeMapper(seqType2, new SequenceElementCountResolver() {
            @Override
            public int getElementCount(CollectionData parent, SequenceType sequenceType) throws IOException {
                return nj;
            }
        });
        assertFalse(type.isSizeKnown());
        assertEquals(-1, type.getSize());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(ni * nj * (8 + 8));
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(format.getByteOrder());
        for (int j = 0; j < nj; j++) {
            for (int i = 0; i < ni; i++) {
                ios.writeDouble(20.0 + 0.1 * i + 0.2 * j);
                ios.writeDouble(40.0 + 0.1 * i + 0.2 * j);
            }
        }
        ios.close();

        final byte[] byteData = baos.toByteArray();
        assertEquals(ni * nj * 2 * 8, byteData.length);
        final IOContext context = new IOContext(format, new ByteArrayIOHandler(byteData));
        final CompoundData compoundData = context.getData();

        assertSame(VarCompound.class, compoundData.getClass());
        assertSame(FixSequenceOfVarCollections.class, compoundData.getSequence(0).getClass());
        assertSame(FixSequenceOfFixCollections.class, compoundData.getSequence(0).getSequence(0).getClass());
        assertSame(FixCompound.class, compoundData.getSequence(0).getSequence(0).getCompound(0).getClass());

        for (int j = 0; j < nj; j++) {
            for (int i = 0; i < ni; i++) {
                assertEquals("i=" + i + ",j=" + j, 20.0 + 0.1 * i + 0.2 * j, compoundData.getSequence(0).getSequence(j).getCompound(i).getDouble(0), 1e-10);
                assertEquals("i=" + i + ",j=" + j, 40.0 + 0.1 * i + 0.2 * j, compoundData.getSequence(0).getSequence(j).getCompound(i).getDouble(1), 1e-10);
            }
        }
    }

    public void testNestedFixSequenceOfVarCollections() throws IOException {

        final int ni = 4;
        final int nj = 2;
        final int nk = 3;
        final SequenceType seqType0 = SEQ(DOUBLE);
        final CompoundType pointType = COMP("Point", MEMBER("Coords", seqType0));
        final SequenceType seqType1 = SEQ(pointType);
        final SequenceType seqType2 = SEQ(seqType1);
        final CompoundType type = COMP("Polygon", MEMBER("PointList", seqType2));
        final Format format = new Format(type, ByteOrder.BIG_ENDIAN);
        format.addSequenceTypeMapper(seqType0, new SequenceElementCountResolver() {
            @Override
            public int getElementCount(CollectionData parent, SequenceType sequenceType) throws IOException {
                return ni;
            }
        });
        format.addSequenceTypeMapper(seqType1, new SequenceElementCountResolver() {
            @Override
            public int getElementCount(CollectionData parent, SequenceType sequenceType) throws IOException {
                return nj;
            }
        });
        format.addSequenceTypeMapper(seqType2, new SequenceElementCountResolver() {
            @Override
            public int getElementCount(CollectionData parent, SequenceType sequenceType) throws IOException {
                return nk;
            }
        });

        assertFalse(type.isSizeKnown());
        assertEquals(-1, type.getSize());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(ni * nj * nk * 8);
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        ios.setByteOrder(format.getByteOrder());
        for (int k = 0; k < nk; k++) {
            for (int j = 0; j < nj; j++) {
                for (int i = 0; i < ni; i++) {
                    ios.writeDouble(10.0 * i + 0.1 * j + 0.2 * k);
                }
            }
        }
        ios.close();
        final byte[] byteData = baos.toByteArray();
        assertEquals(ni * nj * nk * 8, byteData.length);
        final IOContext context = new IOContext(format, new ByteArrayIOHandler(byteData));
        final CompoundData compoundData = context.getData();

        assertSame(VarCompound.class, compoundData.getClass());
        final SequenceData pointListData = compoundData.getSequence("PointList");
        assertSame(FixSequenceOfVarCollections.class, pointListData.getClass());
        final SequenceData pointListDataSeq0 = pointListData.getSequence(0);
        assertSame(FixSequenceOfVarCollections.class, pointListDataSeq0.getClass());
        final CompoundData pointListDataSeq0Comp0 = pointListDataSeq0.getCompound(0);
        assertSame(VarCompound.class, pointListDataSeq0Comp0.getClass());
        final SequenceData pointListDataSeq0Comp0Coords = pointListDataSeq0Comp0.getSequence("Coords");
        assertSame(FixSequenceOfSimples.class, pointListDataSeq0Comp0Coords.getClass());

        for (int k = 0; k < nk; k++) {
            final SequenceData kData = pointListData.getSequence(k);
            for (int j = 0; j < nj; j++) {
                final CompoundData kjData = kData.getCompound(j);
                final SequenceData coordsData = kjData.getSequence("Coords");
                for (int i = 0; i < ni; i++) {
                    System.out.println("i=" + i + ",j=" + j + ",k=" + k);

                    assertEquals("i=" + i + ",j=" + j + ",k=" + k,
                                 10.0 * i + 0.1 * j + 0.2 * k,
                                 coordsData.getDouble(i), 1e-10);
                }
            }
        }
    }
}
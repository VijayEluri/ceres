package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.IOContext;
import com.bc.ceres.binio.Type;

import java.io.IOException;


final class VarCompound extends AbstractCompound {
    private final CompoundType resolvedCompoundType;
    private int maxResolvedIndex;

    public VarCompound(IOContext context, CollectionData parent, CompoundType compoundType, long position) {
        super(context, parent, compoundType, position);
        this.resolvedCompoundType = new CompoundType(compoundType.getName(), compoundType.getMembers());
        this.maxResolvedIndex = -1;

        // todo - OPT: do this in resolve()
        // todo - OPT: do this for all subsequntial fixed-size member types
        // todo - OPT: idea: register a hint with a type in a format whcih tells the com.bc.ceres.binio API to use a fixed size segment

        // Determine first members up to maxMemberIndex which have known size
        int maxMemberIndex = FixCompound.getMemberIndexWithinSizeLimit(compoundType, com.bc.ceres.binio.internal.Segment.SEGMENT_SIZE_LIMIT);
        if (maxMemberIndex >= 0) {
            int segmentSize = 0;
            for (int i = 0; i <= maxMemberIndex; i++) {
                segmentSize += compoundType.getMember(i).getType().getSize();
            }
            Segment segment = new Segment(position, segmentSize);
            int segmentOffset = 0;
            for (int i = 0; i <= maxMemberIndex; i++) {
                final Type memberType = compoundType.getMember(i).getType();
                setMemberInstance(i, InstanceFactory.createFixMember(context, this, memberType, segment, segmentOffset));
                segmentOffset += memberType.getSize();
            }
        }

        maxResolvedIndex = maxMemberIndex;
    }

    public Type getType() {
        return resolvedCompoundType;
    }

    public long getSize() {
        return resolvedCompoundType.getSize();
    }

    @Override
    public boolean isSizeResolved() {
        return resolvedCompoundType.isSizeKnown();
    }

    public boolean isSizeResolved(int index) {
        return index <= maxResolvedIndex;
    }

    public void resolveSize() throws IOException {
        resolveSize(getElementCount() - 1);
    }

    public void resolveSize(int index) throws IOException {
        if (isSizeResolved(index)) {
            return;
        }
        for (int i = maxResolvedIndex + 1; i <= index; i++) {
            MemberInstance memberInstance = getMemberInstance(i);
            if (!memberInstance.isSizeResolved()) {
                memberInstance.resolveSize();
            }
            final CompoundType.Member resolvedMember = new CompoundType.Member(getCompoundType().getMember(i).getName(), memberInstance.getType());
            resolvedCompoundType.setMember(i, resolvedMember);
        }
        if (maxResolvedIndex < index) {
            maxResolvedIndex = index;
        }
    }

    @Override
    protected MemberInstance getMemberInstance(int i) throws IOException {
        MemberInstance memberInstance = super.getMemberInstance(i);
        if (memberInstance == null) {
            memberInstance = createMemberInstance(i);
            setMemberInstance(i, memberInstance);
        }
        return memberInstance;
    }

    private MemberInstance createMemberInstance(int index) throws IOException {
        final IOContext context = getContext();
        final Type memberType = getCompoundType().getMemberType(index);
        final long position;
        if (index > 0) {
            final MemberInstance prevMember = getMemberInstance(index - 1);
            if (!prevMember.isSizeResolved()) {
                prevMember.resolveSize();
            }
            position = prevMember.getPosition() + prevMember.getSize();
        } else {
            position = getPosition();
        }
        return InstanceFactory.createMember(context, this, memberType, position, getContext().getFormat().getByteOrder());
    }
}
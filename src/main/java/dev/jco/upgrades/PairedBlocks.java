package dev.jco.upgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** A two-block chest is one upgrade job, provided both halves and the result support pairing. */
public final class PairedBlocks {
    private PairedBlocks() {}

    @SuppressWarnings("unchecked")
    private static EnumProperty<ChestType> type(BlockState state) {
        var property = state.getBlock().getStateDefinition().getProperty("type");
        return property instanceof EnumProperty<?> enumeration && enumeration.getValue("left").orElse(null) == ChestType.LEFT
            ? (EnumProperty<ChestType>) enumeration : null;
    }

    private static DirectionProperty facing(BlockState state) {
        var property = state.getBlock().getStateDefinition().getProperty("facing");
        return property instanceof DirectionProperty direction ? direction : null;
    }

    @SuppressWarnings("unchecked")
    private static EnumProperty<DoubleBlockHalf> half(BlockState state) {
        var property = state.getBlock().getStateDefinition().getProperty("half");
        return property instanceof EnumProperty<?> enumeration && enumeration.getValue("upper").orElse(null) == DoubleBlockHalf.UPPER
            ? (EnumProperty<DoubleBlockHalf>) enumeration : null;
    }

    public static BlockPos partner(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var half = half(state);
        if(half!=null){
            var upper=state.getValue(half)==DoubleBlockHalf.UPPER;
            var otherPos=upper?pos.below():pos.above();
            if(!level.hasChunkAt(otherPos))return null;
            var other=level.getBlockState(otherPos);var otherHalf=half(other);
            return other.getBlock()==state.getBlock()&&otherHalf!=null&&other.getValue(otherHalf)==(upper?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER)?otherPos:null;
        }
        var type = type(state);
        var facing = facing(state);
        if (type == null || facing == null || state.getValue(type) == ChestType.SINGLE) return null;
        Direction direction = state.getValue(facing);
        BlockPos otherPos = pos.relative(state.getValue(type) == ChestType.LEFT ? direction.getClockWise() : direction.getCounterClockWise());
        if (!level.hasChunkAt(otherPos)) return null;
        var other = level.getBlockState(otherPos);
        var otherType = type(other);
        var otherFacing = facing(other);
        if (other.getBlock() != state.getBlock() || otherType == null || otherFacing == null
            || other.getValue(otherFacing) != direction || other.getValue(otherType) !=
                (state.getValue(type) == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT)) return null;
        return otherPos;
    }

    public static boolean appearsPaired(BlockState state) {
        if(half(state)!=null)return true;
        var type = type(state);
        return type != null && state.getValue(type) != ChestType.SINGLE;
    }

    public static boolean supportsPair(BlockState state) {
        return half(state)!=null||type(state) != null && facing(state) != null;
    }

    public static boolean compatiblePair(BlockState source,BlockState target) {
        return half(source)!=null?half(target)!=null:type(source)!=null&&facing(source)!=null&&type(target)!=null&&facing(target)!=null;
    }

    public static BlockPos owner(ServerLevel level, BlockPos pos) {
        var other = partner(level, pos);
        return other == null || pos.asLong() < other.asLong() ? pos : other;
    }

    public static BlockState single(BlockState state) {
        var type = type(state);
        return type == null ? state : state.setValue(type, ChestType.SINGLE);
    }
}

package com.github.sculkhorde.common.entity.infection;

import com.github.sculkhorde.common.tileentity.SculkNodeTile;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public class SculkNodeInfectionHandler {

    // The parent tile entity
    private SculkNodeTile parent = null;
    private ServerWorld world = null;
    private BlockPos origin = null;

    // The infection trees
    private InfectionTree northInfectionTree;
    private InfectionTree southInfectionTree;
    private InfectionTree eastInfectionTree;
    private InfectionTree westInfectionTree;
    private InfectionTree upInfectionTree;
    private InfectionTree downInfectionTree;


    public SculkNodeInfectionHandler(SculkNodeTile parent) {
        this.parent = parent;
        this.world = (ServerWorld) parent.getLevel();
        this.origin = parent.getBlockPos();

        northInfectionTree = new InfectionTree(world, Direction.NORTH, origin);
        northInfectionTree.activate();

        southInfectionTree = new InfectionTree(world, Direction.SOUTH, origin);
        southInfectionTree.activate();

        eastInfectionTree = new InfectionTree(world, Direction.EAST, origin);
        eastInfectionTree.activate();

        westInfectionTree = new InfectionTree(world, Direction.WEST, origin);
        westInfectionTree.activate();

        upInfectionTree = new InfectionTree(world, Direction.UP, origin);
        upInfectionTree.activate();

        downInfectionTree = new InfectionTree(world, Direction.DOWN, origin);
        downInfectionTree.activate();

    }

    public void tick() {
        northInfectionTree.tick();
        southInfectionTree.tick();
        eastInfectionTree.tick();
        westInfectionTree.tick();
        upInfectionTree.tick();
        downInfectionTree.tick();
    }
}
package com.github.sculkhoard.core.gravemind;


import com.github.sculkhoard.common.block.BlockAlgorithms;
import com.github.sculkhoard.common.entity.SculkLivingEntity;
import com.github.sculkhoard.core.BlockRegistry;
import com.github.sculkhoard.core.SculkHoard;
import com.github.sculkhoard.core.gravemind.entity_factory.EntityFactory;
import com.github.sculkhoard.core.gravemind.entity_factory.ReinforcementContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.sculkhoard.core.SculkHoard.DEBUG_MODE;
import static com.github.sculkhoard.core.SculkHoard.gravemind;

/**
 * This class represents the logistics for the Gravemind and is SEPARATE from the physical version.
 * The gravemind is a state machine that is used to coordinate the sculk hoard.
 * Right now only controls the reinforcement system.
 *
 * Future Plans:
 * -Controls Sculk Raids
 * -Coordinate Defense
 * -Make Coordination of Reinforcements more advanced
 */
public class Gravemind
{

    /** State Logic **/
    public enum evolution_states {Undeveloped, Immature, Mature}

    private evolution_states evolution_state;

    public enum attack_states {Defensive, Offensive}

    public attack_states attack_state = attack_states.Defensive;

    //private GravemindState;

    /** Controlable Assets **/

    //This controls the reinforcement system.
    public static EntityFactory entityFactory;

    public static GravemindMemory gravemindMemory;

    /** Regular Variables **/

    //This is a list of all known positions of sculkNodes.
    //We do not want to put them too close to each other.
    private static final int MINIMUM_DISTANCE_BETWEEN_NODES = 300;

    //This is how much mass is needed to go from undeveloped to immature
    private final int MASS_GOAL_FOR_IMMATURE = 500;
    //This is how much mass is needed to go from immature to mature
    private final int MASS_GOAL_FOR_MATURE = 100000000;

    private final int SCULK_NODE_INFECT_RADIUS_UNDEVELOPED = 10;
    //The radius that sculk nodes can infect in the immature state
    private final int SCULK_NODE_INFECT_RADIUS_IMMATURE = 20;
    //The radius that sculk nodes can infect in the mature state
    private final int SCULK_NODE_INFECT_RADIUS_MATURE = 50;

    public int allowed_nodes = 1;

    //Determines the range which a sculk node can infect land around it
    public int sculk_node_infect_radius = SCULK_NODE_INFECT_RADIUS_UNDEVELOPED;

    public enum EntityDesignation {
        HOSTILE,
        VICTIM
    }

    /**
     * Default Constructor <br>
     * Called in ForgeEventSubscriber.java in world load event. <br>
     * WARNING: DO NOT CALL THIS FUNCTION UNLESS THE WORLD IS LOADED
     */
    public Gravemind()
    {
        evolution_state = evolution_states.Undeveloped;
        attack_state = attack_states.Defensive;
        entityFactory = SculkHoard.entityFactory;
        gravemindMemory = new GravemindMemory();
        calulateCurrentState();
    }

    public evolution_states getEvolutionState()
    {
        return evolution_state;
    }

    public attack_states getAttackState()
    {
        return attack_state;
    }

    /**
     * Used to figure out what state the gravemind is in. Called periodically. <br>
     * Useful for when world is loaded in because we dont store the state.
     */
    public void calulateCurrentState()
    {
        if(SculkHoard.entityFactory.getSculkAccumulatedMass() >= MASS_GOAL_FOR_IMMATURE)
        {
            sculk_node_infect_radius = SCULK_NODE_INFECT_RADIUS_IMMATURE;
            evolution_state = evolution_states.Immature;
        }
        else if(SculkHoard.entityFactory.getSculkAccumulatedMass() >= MASS_GOAL_FOR_MATURE)
        {
            sculk_node_infect_radius = SCULK_NODE_INFECT_RADIUS_MATURE;
            evolution_state = evolution_states.Mature;
        }
    }

    public boolean processReinforcementRequest(ReinforcementContext context)
    {
        context.isRequestViewed = true;

        //Auto approve is this reinforcement is requested by a developer or sculk mass
        if(context.sender == ReinforcementContext.senderType.Developer || context.sender == ReinforcementContext.senderType.SculkMass)
        {
            context.isRequestApproved = true;
        }
        //If gravemind is undeveloped, just auto approve all requests
        else if(evolution_state == evolution_states.Undeveloped)
        {
            context.isRequestApproved = true;
        }
        else if(evolution_state == evolution_states.Immature)
        {
            //Spawn Combat Mobs to deal with player
            if(context.is_aggressor_nearby)
            {
                context.approvedMobTypes.add(EntityFactory.StrategicValues.Melee);
                context.approvedMobTypes.add(EntityFactory.StrategicValues.Ranged);
                context.isRequestApproved = true;
            }

            //Spawn infector mobs to infect
            if(context.is_non_sculk_mob_nearby)
            {
                context.approvedMobTypes.add(EntityFactory.StrategicValues.Infector);
                context.isRequestApproved = true;
            }
        }
        else if(evolution_state == evolution_states.Mature)
        {
            //TODO: Add functionality for mature state
        }

        return context.isRequestApproved;
    }

    /**
     * Determines if a given evolution state is equal to or below the current evolution state.
     * @param stateIn The given state to check
     * @return True if the given state is equal to or less than current evolution state.
     */
    public boolean isEvolutionStateEqualOrLessThanCurrent(evolution_states stateIn)
    {
        if(evolution_state == evolution_states.Undeveloped)
        {
            return (stateIn == evolution_states.Undeveloped);
        }
        else if(evolution_state == evolution_states.Immature)
        {
            return (stateIn == evolution_states.Immature || stateIn == evolution_states.Undeveloped);
        }
        else if(evolution_state == evolution_states.Mature)
        {
            return(stateIn == evolution_states.Undeveloped
                || stateIn == evolution_states.Immature
                || stateIn == evolution_states.Mature);
        }
        return false;
    }


    /**
     * Will only place sculk nodes if sky is visible
     * @param worldIn The World to place it in
     * @param targetPos The position to place it in
     */
    public static void placeSculkNode(ServerWorld worldIn, BlockPos targetPos)
    {
        //Random Chance to Place Node
        if(new Random().nextInt(100) < 1)
        {
            //If we are too close to another node, do not create one
            if(SculkHoard.gravemind.isValidPositionForSculkNode(worldIn, targetPos))
            {
                worldIn.setBlockAndUpdate(targetPos, BlockRegistry.SCULK_BRAIN.get().defaultBlockState());
                SculkHoard.gravemind.gravemindMemory.addNodeToMemory(targetPos, worldIn);
                EntityType.LIGHTNING_BOLT.spawn(worldIn, null, null, targetPos, SpawnReason.SPAWNER, true, true);
                //if(DEBUG_MODE) System.out.println("New Sculk Node Created at " + targetPos);
            }
        }
    }


    /**
     * Will check each known node location in {@link GravemindMemory#nodeEntries}
     * to see if there is one too close.
     * @param positionIn The potential location of a new node
     * @return true if creation of new node is approved, false otherwise.
     */
    public boolean isValidPositionForSculkNode(ServerWorld worldIn, BlockPos positionIn)
    {
        if(!worldIn.canSeeSky(positionIn))
        {
            return false;
        }

        for (NodeEntry entry : gravemindMemory.getNodeEntries())
        {
            //Get Distance from our potential location to the current index node position
            int distanceFromPotentialToCurrentNode = (int) BlockAlgorithms.getBlockDistance(positionIn, entry.position);

            //if we find a single node that is too close, disapprove of creating a new one
            if (distanceFromPotentialToCurrentNode < MINIMUM_DISTANCE_BETWEEN_NODES)
            {
                return false;
            }
        }
        return true;
    }


    /** ######## Classes ######## **/

    public class GravemindMemory extends WorldSavedData
    {
        public static final String NAME = SculkHoard.MOD_ID + "_gravemind_memory";

        public ServerWorld world;

        //Map<The Name of Mob, IsHostile?>
        public Map<String, HostileEntry> hostileEntries;

        //We do not want to put them too close to each other.
        private ArrayList<NodeEntry> nodeEntries; //= new ArrayList<>();

        private ArrayList<BeeNestEntry> beeNestEntries;

        private ArrayList<BlockPos> spawnerPositions;

        /**
         * Default Constructor
         */
        public GravemindMemory()
        {
            super(NAME);
            nodeEntries = new ArrayList<>();
            beeNestEntries = new ArrayList<>();
            hostileEntries = new HashMap<>();
        }

        /**
         * Constructor
         * @param nameIn The name of the
         */
        public GravemindMemory(String nameIn)
        {
            super(nameIn);
        }


        /** Accessors **/

        /**
         * Returns a list of known node positions
         * @return
         */
        public ArrayList<NodeEntry> getNodeEntries()
        {
            return SculkHoard.gravemind.gravemindMemory.nodeEntries;
        }

        /**
         * Returns a list of known bee nest positions
         * @return
         */
        public ArrayList<BeeNestEntry> getBeeNestEntries()
        {
            return SculkHoard.gravemind.gravemindMemory.beeNestEntries;
        }


        /**
         * Returns the map of known hostiles
         * @return
         */
        public Map<String, HostileEntry> getHostileEntries()
        {
            return gravemind.gravemindMemory.hostileEntries;
        }

        /**
         * Will check the positons of all entries to see
         * if they match the parameter.
         * @param position The position to cross reference
         * @return true if in memory, false otherwise
         */
        public boolean isBeeNestPositionInMemory(BlockPos position)
        {
            for(BeeNestEntry entry : getBeeNestEntries())
            {
                if(entry.position == position)
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Will check the positons of all entries to see
         * if they match the parameter.
         * @param position The position to cross reference
         * @return true if in memory, false otherwise
         */
        public boolean isNodePositionInMemory(BlockPos position)
        {
            for(NodeEntry entry : getNodeEntries())
            {
                if(entry.position.equals(position))
                {
                    return true;
                }
            }
            return false;
        }

        /** ######## Modifiers ######## **/

        /**
         * Adds a position to the list if it does not already exist
         * @param positionIn
         */
        public void addNodeToMemory(BlockPos positionIn, ServerWorld worldIn)
        {
            if(!isNodePositionInMemory(positionIn) && getNodeEntries() != null)
            {
                GravemindMemory memory = worldIn.getDataStorage().computeIfAbsent(GravemindMemory::new, GravemindMemory.NAME);
                memory.getNodeEntries().add(new NodeEntry(positionIn));
                memory.setDirty();
            }
        }

        /**
         * Adds a position to the list if it does not already exist
         * @param positionIn
         */
        public void addBeeNestToMemory(BlockPos positionIn, ServerWorld worldIn)
        {
            if(!isBeeNestPositionInMemory(positionIn) && getBeeNestEntries() != null)
            {
                GravemindMemory memory = worldIn.getDataStorage().computeIfAbsent(GravemindMemory::new, GravemindMemory.NAME);
                memory.getBeeNestEntries().add(new BeeNestEntry(positionIn));
                memory.setDirty();
            }
        }

        /**
         * Translate entities to string to make an identifier. <br>
         * This identifier is then stored in memory in a map.
         * @param entityIn The Entity
         * @param worldIn The World
         */
        public void addHostileToMemory(LivingEntity entityIn, ServerWorld worldIn)
        {
            if(entityIn == null || entityIn instanceof SculkLivingEntity || entityIn instanceof CreeperEntity)
            {
                return;
            }

            String identifier = entityIn.getClass().toString();
            if(identifier != null && !identifier.isEmpty())
            {
                GravemindMemory memory = worldIn.getDataStorage().computeIfAbsent(GravemindMemory::new, GravemindMemory.NAME);
                memory.getHostileEntries().putIfAbsent(identifier, new HostileEntry(identifier));
            }
        }


        /** ######## Events ######### **/

        /**
         * Will verify all enties to see if they exist in the world.
         * If not, they will be removed. <br>
         * Gets called in {@link com.github.sculkhoard.util.ForgeEventSubscriber#WorldTickEvent}
         * @param worldIn The World
         */
        public void validateNodeEntries(ServerWorld worldIn)
        {
            long startTime = System.nanoTime();
            for(int index = 0; index < nodeEntries.size(); index++)
            {
                //TODO: Figure out if not being in the overworld can mess this up
                if(!getNodeEntries().get(index).isEntryValid(worldIn))
                {
                    getNodeEntries().remove(index);
                    index--;
                }
            }
            long endTime = System.nanoTime();
            if(DEBUG_MODE) System.out.println("Node Validation Took " + TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS) + " milliseconds");
        }


        /**
         * Will verify all enties to see if they exist in the world.
         * Will also reasses the parentNode for each one.
         * If not, they will be removed. <br>
         * Gets called in {@link com.github.sculkhoard.util.ForgeEventSubscriber#WorldTickEvent}
         * @param worldIn The World
         */
        public void validateBeeNestEntries(ServerWorld worldIn)
        {
            long startTime = System.nanoTime();
            for(int index = 0; index < getBeeNestEntries().size(); index++)
            {
                getBeeNestEntries().get(index).setParentNodeToClosest();
                //TODO: Figure out if not being in the overworld can mess this up
                if(!getBeeNestEntries().get(index).isEntryValid(worldIn))
                {
                    getBeeNestEntries().remove(index);
                    index--;
                }
            }
            long endTime = System.nanoTime();
            if(DEBUG_MODE) System.out.println("Bee Nest Validation Took " + TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS) + " milliseconds");
        }

        @Override
        @Nonnull
        public void load(CompoundNBT nbt)
        {
            if(SculkHoard.gravemind == null)
            {
                SculkHoard.gravemind = new Gravemind();
            }
            CompoundNBT gravemindData = nbt.getCompound("gravemindData");

            getNodeEntries().clear();
            getBeeNestEntries().clear();
            getHostileEntries().clear();

            for(int i = 0; gravemindData.contains("node_entry" + i); i++)
            {
                getNodeEntries().add(NodeEntry.serialize(gravemindData.getCompound("node_entry" + i)));
            }

            for(int i = 0; gravemindData.contains("bee_nest_entry" + i); i++)
            {
                getBeeNestEntries().add(BeeNestEntry.serialize(gravemindData.getCompound("bee_nest_entry" + i)));
            }

            for(int i = 0; gravemindData.contains("hostile_entry" + i); i++)
            {
                //getHostileEntries().add(BeeNestEntry.serialize(gravemindData.getCompound("hostile_entry" + i)));
                HostileEntry hostileEntry = HostileEntry.serialize(gravemindData.getCompound("hostile_entry" + i));
                getHostileEntries().putIfAbsent(hostileEntry.identifier, hostileEntry);
            }

            System.out.print("");
        }

        /**
         * Used to save the {@code SavedData} to a {@code CompoundTag}
         *
         * @param nbt the {@code CompoundTag} to save the {@code SavedData} to
         */
        @Override
        @Nonnull
        public CompoundNBT save(CompoundNBT nbt)
        {
            CompoundNBT gravemindData = new CompoundNBT();

            for(ListIterator<NodeEntry> iterator = getNodeEntries().listIterator(); iterator.hasNext();)
            {
                gravemindData.put("node_entry" + iterator.nextIndex(),iterator.next().deserialize());
            }

            for(ListIterator<BeeNestEntry> iterator = getBeeNestEntries().listIterator(); iterator.hasNext();)
            {
                gravemindData.put("bee_nest_entry" + iterator.nextIndex(),iterator.next().deserialize());
            }

            int hostileIndex = 0;
            for(Map.Entry<String, HostileEntry> entry : getHostileEntries().entrySet())
            {
                gravemindData.put("hostile_entry" + hostileIndex, entry.getValue().deserialize());
                hostileIndex++;
            }


            nbt.put("gravemindData", gravemindData);
            return nbt;
        }


    }



    /** ######## CLASSES ######### **/

    /**
     * This class is a representation of the actual
     * Sculk Nodes in the world that the horde has access
     * to. It allows the gravemind to keep track of all.
     */
    private static class NodeEntry
    {
        private BlockPos position; //The Location in the world where the node is
        private long lastTimeWasActive; //The Last Time A node was active and working

        /**
         * Default Constructor
         * @param positionIn The physical location
         */
        public NodeEntry(BlockPos positionIn)
        {
            position = positionIn;
            lastTimeWasActive = System.nanoTime();
        }

        public boolean isEntryValid(ServerWorld worldIn)
        {
            if(worldIn.getBlockState(position).getBlock().is(BlockRegistry.SCULK_BRAIN.get()))
            {
                return true;
            }
            return false;
        }

        public CompoundNBT deserialize()
        {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putLong("position", position.asLong());
            nbt.putLong("lastTimeWasActive", lastTimeWasActive);
            return nbt;
        }

        public static NodeEntry serialize(CompoundNBT nbt)
        {
            return new NodeEntry(BlockPos.of(nbt.getLong("position")));
        }

    }

    /**
     * This class is a representation of the actual
     * Bee Nests in the world that the horde has access
     * to. It allows the gravemind to keep track of all.
     */
    private static class BeeNestEntry
    {
        private BlockPos position; //The location in the world where the node is
        private BlockPos parentNodePosition; //The location of the Sculk Node that this Nest belongs to

        /**
         * Default Constructor
         * @param positionIn The Position of this Nest
         */
        public BeeNestEntry(BlockPos positionIn)
        {
            position = positionIn;
        }

        public BeeNestEntry(BlockPos positionIn, BlockPos parentPositionIn)
        {
            position = positionIn;
            parentNodePosition = parentPositionIn;
        }

        /**
         * Checks if the block does still exist in the world.
         * @param worldIn The world to check
         * @return True if valid, false otherwise.
         */
        public boolean isEntryValid(ServerWorld worldIn)
        {
            if(worldIn.getBlockState(position).getBlock().is(BlockRegistry.SCULK_BEE_NEST_BLOCK.get()))
            {
                return true;
            }
            return false;
        }

        /**
         * Checks list of node entries and finds the closest one.
         * It then sets the parentNodePosition to be the position of
         * the closest node.
         */
        public void setParentNodeToClosest()
        {
            //Make sure nodeEntries isnt null and nodeEntries isnt empty
            if(SculkHoard.gravemind.gravemindMemory.getNodeEntries() != null && !SculkHoard.gravemind.gravemindMemory.getNodeEntries().isEmpty())
            {
                NodeEntry closestEntry = SculkHoard.gravemind.gravemindMemory.getNodeEntries().get(0);
                for(NodeEntry entry : SculkHoard.gravemind.gravemindMemory.getNodeEntries())
                {
                    //If entry is closer than our current closest entry
                    if(BlockAlgorithms.getBlockDistance(position, entry.position) < BlockAlgorithms.getBlockDistance(position, closestEntry.position))
                    {
                        closestEntry = entry;
                    }
                }
                parentNodePosition = closestEntry.position;
            }
        }

        public CompoundNBT deserialize()
        {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putLong("position", position.asLong());
            if(parentNodePosition != null) nbt.putLong("parentNodePosition", parentNodePosition.asLong());
            return nbt;
        }

        public static BeeNestEntry serialize(CompoundNBT nbt)
        {
            return new BeeNestEntry(BlockPos.of(nbt.getLong("position")), BlockPos.of(nbt.getLong("parentNodePosition")));
        }
    }


    /**
     * This class is a representation of the actual
     * Sculk Nodes in the world that the horde has access
     * to. It allows the gravemind to keep track of all.
     */
    private static class HostileEntry
    {
        private String identifier; //The String that is the class name identifier of the mob. Example: class net.minecraft.entity.monster.SpiderEntity

        /**
         * Default Constructor
         * @param identifierIn The String that is the class name identifier of the mob. <br>
         * Example: class net.minecraft.entity.monster.SpiderEntity
         */
        public HostileEntry(String identifierIn)
        {
            identifier = identifierIn;
        }

        public CompoundNBT deserialize()
        {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putString("identifier", identifier);
            return nbt;
        }

        public static HostileEntry serialize(CompoundNBT nbt)
        {
            return new HostileEntry(nbt.getString("identifier"));
        }

    }

}
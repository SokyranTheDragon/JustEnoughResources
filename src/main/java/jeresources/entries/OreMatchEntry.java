package jeresources.entries;

import jeresources.api.distributions.DistributionBase;
import jeresources.api.messages.RegisterOreMessage;
import jeresources.api.utils.ColorHelper;
import jeresources.api.utils.DistributionHelpers;
import jeresources.api.utils.DropItem;
import jeresources.api.utils.restrictions.Restriction;
import jeresources.compatibility.Compatibility;
import jeresources.config.Settings;
import jeresources.utils.MapKeys;
import net.minecraft.item.ItemStack;

import java.util.*;


public class OreMatchEntry
{
    private float[] chances;
    Map<String, Boolean> silkTouchMap = new LinkedHashMap<String, Boolean>();
    List<OreEntry> oreSet = new ArrayList<OreEntry>();
    private int minY;
    private int maxY;
    private int bestY;
    private boolean denseOre;
    private int colour;
    private Restriction restriction;
    List<DropItem> drops = new ArrayList<>();

    public OreMatchEntry(RegisterOreMessage message)
    {
        restriction = message.getRestriction();
        addMessage(message);
    }

    private void addMessage(RegisterOreMessage message)
    {
        silkTouchMap.put(MapKeys.key(message.getOre()), message.needSilkTouch());
        oreSet.add(new OreEntry(message.getOre(), message.getDistribution()));
        calcChances();
        if (colour == ColorHelper.BLACK) colour = message.getColour();
    }

    public boolean add(RegisterOreMessage message)
    {
        if (!restriction.isMergeable(message.getRestriction())) return false;
        addMessage(message);
        return message.getRestriction().equals(this.restriction);
    }

    public void add(OreMatchEntry oreMatchEntry)
    {
        if (restriction.isMergeable(oreMatchEntry.restriction))
        {
            silkTouchMap.putAll(oreMatchEntry.silkTouchMap);
            oreSet.addAll(oreMatchEntry.oreSet);
            denseOre |= oreMatchEntry.denseOre;
            calcChances();
            if (colour == ColorHelper.BLACK) colour = oreMatchEntry.getColour();
        }
    }

    private void calcChances()
    {
        chances = new float[256];
        minY = 256;
        maxY = 0;
        for (OreEntry entry : oreSet)
        {
            DistributionBase distribution = entry.getDistribution();
            int i = -1;
            for (float chance : distribution.getDistribution())
            {
                if (++i == chances.length) break;
                chances[i] += chance * (denseOre && i < 81 ? Compatibility.DENSE_ORES_MULTIPLIER : 1);
                if (chances[i] > 0)
                {
                    if (minY > i)
                        minY = i;
                    if (i > maxY)
                        maxY = i;
                }
            }
            bestY = distribution.getBestHeight();
        }
        if (minY == 256) minY = 0;
        if (maxY == 0) maxY = 255;
        if (oreSet.size() > 1) bestY = DistributionHelpers.calculateMeanLevel(chances, 40);
    }

    public float[] getChances()
    {
        return getChances(Settings.EXTRA_RANGE);
    }

    public float[] getChances(int extraRange)
    {
        return Arrays.copyOfRange(chances, Math.max(minY - extraRange, 0), Math.min(maxY + extraRange + 2, 255));
    }

    public int getBestY()
    {
        return bestY;
    }

    public int getMinY()
    {
        return minY;
    }

    public int getMaxY()
    {
        return maxY;
    }

    public boolean isSilkTouchNeeded(ItemStack itemStack)
    {
        Boolean silkTouch = this.silkTouchMap.get(MapKeys.key(itemStack));
        return silkTouch == null ? false : silkTouch;
    }

    public int getColour()
    {
        return colour;
    }

    public void addDrop(DropItem nonOre)
    {
        drops.add(nonOre);
        boolean silkTouch = false;
        if (MapKeys.getKey(nonOre).startsWith("denseore"))
        {
            denseOre = true;
            silkTouch = true;
            calcChances();
        }
        silkTouchMap.put(MapKeys.getKey(nonOre), silkTouch);
    }

    public void removeDrop(ItemStack removeDrop)
    {
        for (Iterator<DropItem> itr = drops.iterator(); itr.hasNext(); )
        {
            DropItem drop = itr.next();
            if (drop.item.isItemEqual(removeDrop))
            {
                silkTouchMap.remove(MapKeys.getKey(drop));
                itr.remove();
            }
        }
        if (MapKeys.getKey(removeDrop).startsWith("denseore"))
        {
            denseOre = false;
            calcChances();
        }
    }

    public List<DropItem> getDrops()
    {
        return drops;
    }

    public List<ItemStack> getOresAndDrops()
    {
        List<ItemStack> list = new LinkedList<ItemStack>();
        for (OreEntry entry : oreSet)
        {
            ItemStack ore = entry.getOre();
            if (!list.contains(ore)) list.add(ore);
        }

        for (DropItem dropItem : drops)
        {
            ItemStack drop = dropItem.item;
            if (!list.contains(drop)) list.add(drop);
        }

        return list;
    }

    public List<ItemStack> getOres()
    {
        List<ItemStack> list = new LinkedList<ItemStack>();
        for (OreEntry entry : oreSet)
        {
            ItemStack ore = entry.getOre();
            if (!list.contains(ore)) list.add(ore);
        }
        return list;
    }

    public List<String> getRestrictions()
    {
        return this.restriction.getStringList(Settings.useDimNames);
    }

    public boolean hasDrop(ItemStack itemStack)
    {
        for (DropItem drop : getDrops())
            if (drop.item.isItemEqual(itemStack))
                return true;
        return false;
    }

    public boolean hasOre(ItemStack itemStack)
    {
        for (ItemStack ore : getOres())
            if (ore.isItemEqual(itemStack))
                return true;
        return false;
    }

    public DropItem getDropItem(ItemStack itemStack)
    {
        for (DropItem drop : getDrops())
            if (drop.item.isItemEqual(itemStack))
                return drop;
        return null;
    }

    @Override
    public String toString()
    {
        return "Match: " + oreSet.get(0).getOre().getDisplayName() + " - " + restriction.toString();
    }

    private class OreEntry
    {
        private ItemStack ore;
        private DistributionBase distribution;

        public OreEntry(ItemStack ore, DistributionBase distribution)
        {
            this.ore = ore;
            this.distribution = distribution;
        }

        public ItemStack getOre()
        {
            return ore;
        }

        public DistributionBase getDistribution()
        {
            return distribution;
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.core.genetics;

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IAlleleTemplateBuilder;
import forestry.api.genetics.IBlockTranslator;
import forestry.api.genetics.IChromosome;
import forestry.api.genetics.IChromosomeType;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.IItemTranslator;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.ISpeciesRoot;
import forestry.core.genetics.alleles.AlleleTemplateBuilder;

public abstract class SpeciesRoot implements ISpeciesRoot {
	/* RESEARCH */
	private final LinkedHashMap<ItemStack, Float> researchCatalysts = new LinkedHashMap<>();

	@Override
	public Map<ItemStack, Float> getResearchCatalysts() {
		return Collections.unmodifiableMap(researchCatalysts);
	}

	@Override
	public void setResearchSuitability(ItemStack itemstack, float suitability) {
		researchCatalysts.put(itemstack, suitability);
	}

	/* TEMPLATES */
	protected final HashMap<String, IAllele[]> speciesTemplates = new HashMap<>();

	@Override
	public Map<String, IAllele[]> getGenomeTemplates() {
		return speciesTemplates;
	}

	@Override
	public void registerTemplate(IAllele[] template) {
		Preconditions.checkNotNull(template, "Tried to register null template");
		Preconditions.checkArgument(template.length > 0, "Tried to register empty template");

		registerTemplate(template[0].getUID(), template);
	}

	@Override
	public IAllele[] getRandomTemplate(Random rand) {
		Collection<IAllele[]> templates = speciesTemplates.values();
		int size = templates.size();
		IAllele[][] templatesArray = templates.toArray(new IAllele[size][]);
		return templatesArray[rand.nextInt(size)];
	}

	@Override
	public IAllele[] getTemplate(String identifier) {
		IAllele[] template = speciesTemplates.get(identifier);
		if (template == null) {
			return null;
		}
		return Arrays.copyOf(template, template.length);
	}

	@Override
	public IAllele[] getTemplate(IAlleleSpecies species) {
		IAllele[] template = getTemplate(species.getUID());
		if (template == null) {
			throw new IllegalStateException("No template found for species " + species.getUID());
		}
		return template;
	}
	
	/* TRANSLATORS */
	private final HashMap<Block, IBlockTranslator> blockTranslators = new HashMap<>();
	private final HashMap<Item, IItemTranslator> itemTranslators = new HashMap<>();

	@Override
	public void registerTranslator(Block translatorKey, IBlockTranslator translator) {
		blockTranslators.putIfAbsent(translatorKey, translator);
	}

	@Override
	public void registerTranslator(Item translatorKey, IItemTranslator translator) {
		itemTranslators.putIfAbsent(translatorKey, translator);
	}

	@Nullable
	@Override
	public IItemTranslator getTranslator(Item translatorKey) {
		return itemTranslators.get(translatorKey);
	}

	@Nullable
	@Override
	public IBlockTranslator getTranslator(Block translatorKey) {
		return blockTranslators.get(translatorKey);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Nullable
	@Override
	public <I extends IIndividual> I translateMember(IBlockState objectToTranslate) {
		Block translatorKey = objectToTranslate.getBlock();
		IBlockTranslator<I> translator = getTranslator(translatorKey);
		if(translator == null){
			return null;
		}
		return translator.getIndividualFromObject(objectToTranslate);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Nullable
	@Override
	public <I extends IIndividual> I translateMember(ItemStack objectToTranslate) {
		Item translatorKey = objectToTranslate.getItem();
		IItemTranslator<I> translator = getTranslator(translatorKey);
		if(translator == null){
			return null;
		}
		return translator.getIndividualFromObject(objectToTranslate);
	}

	@Override
	public ItemStack getGeneticEquivalent(ItemStack objectToTranslate) {
		Item translatorKey = objectToTranslate.getItem();
		IItemTranslator translator = getTranslator(translatorKey);
		if(translator == null){
			return ItemStack.EMPTY;
		}
		return translator.getGeneticEquivalent(objectToTranslate);
	}

	@Override
	public ItemStack getGeneticEquivalent(IBlockState objectToTranslate) {
		Block translatorKey = objectToTranslate.getBlock();
		IBlockTranslator translator = getTranslator(translatorKey);
		if(translator == null){
			return ItemStack.EMPTY;
		}
		return translator.getGeneticEquivalent(objectToTranslate);
	}

	/* MEMBERS */
	@Override
	public boolean isMember(ItemStack stack) {
		return getType(stack) != null;
	}

	/* MUTATIONS */
	@Override
	public List<IMutation> getCombinations(IAllele other) {
		List<IMutation> combinations = new ArrayList<>();
		for (IMutation mutation : getMutations(false)) {
			if (mutation.isPartner(other)) {
				combinations.add(mutation);
			}
		}

		return combinations;
	}

	@Override
	public List<? extends IMutation> getResultantMutations(IAllele other) {
		List<IMutation> mutations = new ArrayList<>();
		int speciesIndex = getSpeciesChromosomeType().ordinal();
		for (IMutation mutation : getMutations(false)) {
			IAllele[] template = mutation.getTemplate();
			if(template == null || template.length <= speciesIndex){
				continue;
			}
			IAllele speciesAllele = template[speciesIndex];
			if (speciesAllele == other) {
				mutations.add(mutation);
			}
		}

		return mutations;
	}

	@Override
	public List<IMutation> getCombinations(IAlleleSpecies parentSpecies0, IAlleleSpecies parentSpecies1, boolean shuffle) {
		List<IMutation> combinations = new ArrayList<>();

		String parentSpecies1UID = parentSpecies1.getUID();
		for (IMutation mutation : getMutations(shuffle)) {
			if (mutation.isPartner(parentSpecies0)) {
				IAllele partner = mutation.getPartner(parentSpecies0);
				if (partner.getUID().equals(parentSpecies1UID)) {
					combinations.add(mutation);
				}
			}
		}

		return combinations;
	}

	@Override
	public Collection<? extends IMutation> getPaths(IAllele result, IChromosomeType chromosomeType) {
		ArrayList<IMutation> paths = new ArrayList<>();
		for (IMutation mutation : getMutations(false)) {
			if (mutation.getTemplate()[chromosomeType.ordinal()] == result) {
				paths.add(mutation);
			}
		}

		return paths;
	}

	/* GENOME CONVERSIONS */
	@Override
	public IChromosome[] templateAsChromosomes(IAllele[] template) {
		Chromosome[] chromosomes = new Chromosome[template.length];
		for (int i = 0; i < template.length; i++) {
			if (template[i] != null) {
				chromosomes[i] = new Chromosome(template[i]);
			}
		}

		return chromosomes;
	}

	@Override
	public IChromosome[] templateAsChromosomes(IAllele[] templateActive, IAllele[] templateInactive) {
		Chromosome[] chromosomes = new Chromosome[templateActive.length];
		for (int i = 0; i < templateActive.length; i++) {
			if (templateActive[i] != null) {
				chromosomes[i] = new Chromosome(templateActive[i], templateInactive[i]);
			}
		}

		return chromosomes;
	}

	@Override
	public IAlleleTemplateBuilder createTemplateBuilder() {
		return new AlleleTemplateBuilder(this);
	}

	@Override
	public IAlleleTemplateBuilder createTemplateBuilder(IAllele[] alleles) {
		return new AlleleTemplateBuilder(this, alleles);
	}
}

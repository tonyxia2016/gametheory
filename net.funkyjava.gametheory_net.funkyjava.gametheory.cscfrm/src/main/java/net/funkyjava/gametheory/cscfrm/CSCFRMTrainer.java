package net.funkyjava.gametheory.cscfrm;

import com.google.common.util.concurrent.AtomicDoubleArray;

import net.funkyjava.gametheory.extensiveformgame.ActionNode;

public class CSCFRMTrainer {

	private final int nbRounds;
	private final int nbPlayers;
	private final ActionNode rootNode;
	private final CSCFRMNode[][][][] nodes;
	private final CSCFRMNode[][][] chancesNodes;
	private final AtomicDoubleArray utilitySum;

	private final double[] realizationWeights;
	private final double[][] depthUtil;
	private final double[][] depthActionUtil;
	private final double[][] depthStrategy;
	private final double[] zero;

	public CSCFRMTrainer(final CSCFRMData data) {
		final int maxDepth = data.maxDepth;
		final int maxNbActions = data.maxNbActions;
		final int nbRounds = this.nbRounds = data.roundChancesSizes.length;
		final int nbPlayers = this.nbPlayers = data.nbPlayers;
		this.utilitySum = data.utilitySum;
		rootNode = data.rootNode;
		nodes = data.nodes;
		chancesNodes = new CSCFRMNode[nbRounds][nbPlayers][];
		zero = new double[Math.max(nbPlayers, maxNbActions)];
		depthUtil = new double[maxDepth][maxNbActions];
		depthActionUtil = new double[maxDepth][maxNbActions];
		depthStrategy = new double[maxDepth][nbPlayers];
		realizationWeights = new double[nbPlayers];
	}

	public final void train(final int[][] chances) {
		// Get the nodes we need for this iteration given the provided chances
		final int nbRounds = this.nbRounds;
		final int nbPlayers = this.nbPlayers;
		final CSCFRMNode[][][][] nodes = this.nodes;
		final CSCFRMNode[][][] chancesNodes = this.chancesNodes;
		for (int round = 0; round < nbRounds; round++) {
			final int[] roundChances = chances[round];
			final CSCFRMNode[][][] roundNodes = nodes[round];
			final CSCFRMNode[][] roundChancesNodes = chancesNodes[round];
			for (int player = 0; player < nbPlayers; player++) {
				roundChancesNodes[player] = roundNodes[player][roundChances[player]];
			}
		}
		final double[] realizationWeights = this.realizationWeights;
		System.arraycopy(zero, 0, realizationWeights, 0, nbPlayers);
		final double[] utility = rec(0, rootNode, chances, realizationWeights);
		final AtomicDoubleArray utilitySum = this.utilitySum;
		for (int i = 0; i < nbPlayers; i++) {
			utilitySum.addAndGet(i, utility[i]);
		}
	}

	private final double[] rec(final int depth, final ActionNode node, final int[][] chances,
			final double[] realizationWeights) {
		switch (node.nodeType) {

		case PAYOUTS_NO_CHANCE:
			return node.payoutsNoChance;

		case CHANCES_PAYOUTS:
			return node.chancesPayouts.getPayouts(chances);

		case PLAYER:
			final int nbPlayers = this.nbPlayers;
			final int index = node.index;
			final int round = node.round;
			final int player = node.player;
			final CSCFRMNode csNode = chancesNodes[round][player][index];

			final int nbChildren = node.nbChildren;
			final ActionNode[] children = node.children;
			final double[] stratSum = csNode.strategySum;
			final double[] regretSum = csNode.regretSum;
			final double[] zero = this.zero;
			final double[] strategy = depthStrategy[depth];
			System.arraycopy(zero, 0, strategy, 0, nbChildren);
			final double[] util = depthUtil[depth];
			System.arraycopy(zero, 0, util, 0, nbPlayers);
			final double[] actionsUtil = depthActionUtil[depth];
			System.arraycopy(zero, 0, actionsUtil, 0, nbChildren);

			double totalRegret = 0;
			for (int action = 0; action < nbChildren; action++) {
				final double actionRegret = regretSum[action];
				totalRegret += strategy[action] = (actionRegret > 0 ? actionRegret : 0);
			}
			final double playerRealWeight = realizationWeights[player];
			if (totalRegret > 0) {
				for (int action = 0; action < nbChildren; action++) {
					stratSum[action] += playerRealWeight * strategy[action] / totalRegret;
				}
			} else {
				for (int action = 0; action < nbChildren; action++) {
					stratSum[action] += playerRealWeight * (strategy[action] = 1.0d / nbChildren);
				}
			}
			final int nextDepth = depth + 1;
			for (int action = 0; action < nbChildren; action++) {
				final double oldReal = realizationWeights[player];
				realizationWeights[player] *= strategy[action];
				final double[] childUtil = rec(nextDepth, children[action], chances, realizationWeights);
				for (int p = 0; p < nbPlayers; p++) {
					util[p] += strategy[action] * childUtil[p];
				}
				actionsUtil[action] = childUtil[player];
				realizationWeights[player] = oldReal;
			}
			double weight = 1;
			for (int p = 0; p < nbPlayers; p++) {
				if (p != player)
					weight *= realizationWeights[p];
			}
			final double playerUtil = util[player];
			for (int action = 0; action < nbChildren; action++) {
				regretSum[action] += weight * (actionsUtil[action] - playerUtil);
			}
			return util;
		}
		return null;
	}

}

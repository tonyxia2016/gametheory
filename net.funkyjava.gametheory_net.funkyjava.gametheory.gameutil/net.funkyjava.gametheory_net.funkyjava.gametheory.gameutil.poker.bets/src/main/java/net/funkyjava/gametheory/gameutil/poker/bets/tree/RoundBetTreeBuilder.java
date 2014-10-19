package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.model.BetNode;

@Slf4j
public class RoundBetTreeBuilder {
	private final List<BetNode> startBetNodes = new LinkedList<>();
	private final List<BetNode> betNodes = new LinkedList<>();

	public int findOrCreateStartBetNode(BetNode node) {
		if (startBetNodes.contains(node)) {
			return betNodes.indexOf(node);
		}
		startBetNodes.add(node);
		betNodes.add(node);
		return betNodes.size() - 1;
	}

	public int createBetNode(BetNode node) {
		betNodes.add(node);
		return betNodes.size() - 1;
	}

	private int[][] getBetNodes() {
		int[][] res = new int[betNodes.size()][];
		for (int i = 0; i < res.length; i++) {
			BetNode node = betNodes.get(i);
			final int nbActions = node.getBets().length;
			final int[] arr = res[i] = new int[nbActions * 2 + 1];
			arr[nbActions * 2] = node.getPlayer();
			System.arraycopy(node.getNextNodes(), 0, arr, 0, nbActions);
			System.arraycopy(node.getBets(), 0, arr, nbActions, nbActions);
		}
		return res;
	}

	public RoundBetTree build() {
		return new RoundBetTree(getBetNodes());
	}
}

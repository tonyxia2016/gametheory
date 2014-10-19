package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import java.util.List;

import net.funkyjava.gametheory.gameutil.poker.bets.pots.Pot;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetChoice;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.PlayersData;

/**
 * 
 * @author Pierre Mardon
 * 
 */
public interface BetRangeSlicer {

	public static final int fold = Integer.MIN_VALUE;

	/**
	 * @param pots
	 *            current pots
	 * @param data
	 *            round data : stacks, bets...
	 * @param choice
	 *            data about possible bets
	 * @param raiseIndex
	 *            depth of the raise sequence :
	 *            <p>
	 *            bet = 0 : the current move can be the first bet, there was no
	 *            blinds;<br>
	 *            bet = 1 there has been a bet or a big blind, the current move
	 *            can be the first raise;<br>
	 *            and so on
	 *            </p>
	 * @param betRoundIndex
	 *            0 for preflop, 1 flop, 2 turn, 3 river for HE
	 * @return the authorized bet values, call and fold (bet of {@link #fold}
	 *         value) included
	 */
	int[] slice(List<Pot<Integer>> pots, PlayersData data, BetChoice choice,
			int raiseIndex, int betRoundIndex);
}

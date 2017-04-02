package net.funkyjava.gametheory.games.nlhe.preflop;

import net.funkyjava.gametheory.games.nlhe.NLHEEquityProvider;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopEquityTables;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopReducedEquityTable;

public class NLHE3PlayersPreflopEquityProvider implements NLHEEquityProvider {

	private final ThreePlayersPreflopReducedEquityTable table;

	public NLHE3PlayersPreflopEquityProvider(final ThreePlayersPreflopReducedEquityTable table) {
		this.table = table;
	}

	@Override
	public double[] getEquity(int betRoundIndex, int[][] roundsPlayersChances, boolean[] playersToConsider) {
		int index = ThreePlayersPreflopEquityTables.heroVilain1Vilain2Index;
		if (!playersToConsider[0]) {
			index = ThreePlayersPreflopEquityTables.vilain1Vilain2Index;
		} else if (!playersToConsider[1]) {
			index = ThreePlayersPreflopEquityTables.heroVilain2Index;
		} else if (!playersToConsider[2]) {
			index = ThreePlayersPreflopEquityTables.heroVilain1Index;
		}
		final int[] chances = roundsPlayersChances[0];
		return table.getEquities(chances[0], chances[1], chances[2])[index];
	}

}

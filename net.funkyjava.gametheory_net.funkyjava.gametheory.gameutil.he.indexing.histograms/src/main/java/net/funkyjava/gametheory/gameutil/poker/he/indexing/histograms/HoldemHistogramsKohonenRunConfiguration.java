package net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
import org.apache.commons.math3.ml.neuralnet.FeatureInitializer;
import org.apache.commons.math3.ml.neuralnet.FeatureInitializerFactory;

import net.funkyjava.gametheory.gameutil.cards.Deck52Cards;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms.HoldemHistograms.HoldemHistogramsStreets;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.histograms.HoldemHistograms.HoldemHistogramsValues;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

public abstract class HoldemHistogramsKohonenRunConfiguration implements HistogramsKohonenRunConfiguration {

	private static final class PreflopHistogramIndexGenerator implements HistogramIndexGenerator {

		private final WaughIndexer indexer = new WaughIndexer(new int[] { 2 });
		private final Random random = new Random();
		private final int[][] cardsGroups = new int[][] { new int[2] };
		private final int[] cards = cardsGroups[0];

		@Override
		public int getHistogramIndex() {
			final int firstCard = cards[0] = random.nextInt(52);
			final int secondCard = cards[1] = random.nextInt(51);

			if (secondCard >= firstCard) {
				cards[1]++;
			}
			return indexer.indexOf(cardsGroups);
		}

	}

	private static final class HoldemHistogramIndexGenerator implements HistogramIndexGenerator {

		private final int boardSize;
		private final WaughIndexer indexer;
		private final int[][] cardsGroups;
		private final int[] preflop;
		private final int[] board;
		private final int[] tmp;

		private final Deck52Cards deck;

		public HoldemHistogramIndexGenerator(final HoldemHistogramsStreets street) {
			switch (street) {
			case FLOP:
				boardSize = 3;
				break;
			case TURN:
				boardSize = 4;
				break;
			default:
				throw new IllegalArgumentException("Wrong street " + street);
			}
			indexer = new WaughIndexer(new int[] { 2, boardSize });
			cardsGroups = new int[][] { new int[2], new int[boardSize] };
			preflop = cardsGroups[0];
			board = cardsGroups[1];
			tmp = new int[2 + boardSize];
			deck = new Deck52Cards(indexer.getCardsSpec());
		}

		@Override
		public int getHistogramIndex() {
			deck.oneShotDeckDraw(tmp);
			System.arraycopy(tmp, 0, preflop, 0, 2);
			System.arraycopy(tmp, 2, board, 0, boardSize);
			return indexer.indexOf(cardsGroups);
		}

	}

	private final int featuresSize;
	private final int samplesSize;
	private final int nbTasks;
	private final DistanceMeasure distance;
	private final double[][] histograms;
	private final HoldemHistogramsStreets street;

	public HoldemHistogramsKohonenRunConfiguration(final HoldemHistogramsStreets street,
			final HoldemHistogramsValues nextStreetValue, final int featureSize, final int sampleSize,
			final int nbTasks) throws IOException, ClassNotFoundException {
		this.street = street;
		this.featuresSize = featureSize;
		this.samplesSize = sampleSize;
		this.nbTasks = nbTasks;
		histograms = HoldemHistograms.generateHistograms(street, nextStreetValue, featuresSize);
		distance = new EarthMoversDistance();
	}

	@Override
	public DistanceMeasure getDistance() {
		return distance;
	}

	@Override
	public double[][] getHistograms() {
		return histograms;
	}

	@Override
	public int getSamplesSize() {
		return samplesSize;
	}

	@Override
	public int getNbTasks() {
		return nbTasks;
	}

	@Override
	public HistogramIndexGenerator getIndexGenerator() {
		switch (street) {
		case FLOP:
		case TURN:
			return new HoldemHistogramIndexGenerator(street);
		case PREFLOP:
			return new PreflopHistogramIndexGenerator();
		}
		return null;
	}

	@Override
	public FeatureInitializer[] getFeaturesInitializers() {
		final FeatureInitializer[] res = new FeatureInitializer[featuresSize];
		for (int i = 0; i < featuresSize; i++) {
			res[i] = FeatureInitializerFactory.uniform(0.1, 1);
		}
		return res;
	}

}

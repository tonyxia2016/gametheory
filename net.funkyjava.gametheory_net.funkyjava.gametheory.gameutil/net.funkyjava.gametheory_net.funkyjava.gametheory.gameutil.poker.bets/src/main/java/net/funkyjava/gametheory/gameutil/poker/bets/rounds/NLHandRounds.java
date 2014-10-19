/**
 * 
 */
package net.funkyjava.gametheory.gameutil.poker.bets.rounds;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.Pot;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.SharedPot;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.anteround.AnteRound;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.anteround.AnteValue;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetChoice;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetRoundStartData;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.nolimit.NLBetRound;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.blindsround.BlindValue;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.blindsround.BlindsRound;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.BlindsAnteParameters;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.PlayersData;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * @author Pierre Mardon
 * 
 */
@Slf4j
public class NLHandRounds implements Cloneable {

	/**
	 * 
	 */
	private AnteRound anteRound;
	private BlindsRound blindsRound;
	private final NLBetRound betRounds[];
	private final boolean hasAnte;
	private final boolean hasBlinds;
	private final boolean isCash;
	private final int roundOffset;
	@Getter
	private final int nbRounds;
	@Getter
	private final int nbBetRounds;
	private RoundType rType;
	@Getter
	private int round = -1;
	private final BlindsAnteParameters params;
	private final int firstPlayerBetRounds;

	private NLHandRounds(NLHandRounds src) {
		anteRound = cloneOrNull(src.anteRound);
		blindsRound = cloneOrNull(src.blindsRound);
		betRounds = new NLBetRound[src.betRounds.length];
		for (int i = 0; i < src.betRounds.length; i++)
			betRounds[i] = cloneOrNull(src.betRounds[i]);
		hasAnte = src.hasAnte;
		hasBlinds = src.hasBlinds;
		isCash = src.isCash;
		roundOffset = src.roundOffset;
		nbRounds = src.nbRounds;
		nbBetRounds = src.nbBetRounds;
		rType = src.rType;
		round = src.round;
		params = src.params;
		firstPlayerBetRounds = src.firstPlayerBetRounds;
	}

	@SuppressWarnings("unchecked")
	private <C extends Object> C cloneOrNull(C obj) {
		if (obj == null)
			return null;
		try {
			return (C) obj.getClass().getMethod("clone").invoke(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public NLHandRounds(BlindsAnteParameters params, int nbBetRounds,
			int firstPlayerBetRounds, boolean isCash) {
		checkArgument(nbBetRounds > 0, "You must have at least one bet round");
		checkArgument(!params.isEnableAnte() || params.getAnteValue() > 0,
				"Ante value {} is invalid", params.getAnteValue());
		checkArgument(
				!params.isEnableBlinds()
						||

						(params.getBbIndex() > 0 && params.getBbValue() > 0 &&

						(params.getSbIndex() < 0 || (params.getSbValue() > 0 && params.getSbValue() <= params.getBbValue()))

						), "The blinds specs are invalid, check them : {}",
				params);
		if (params.isEnableBlinds() && isCash)
			checkNotNull(params.getShouldPostEnteringBb(),
					"In cash game, you must provide booleans to know what player"
							+ " has to pay the bb to enter the hand");
		this.firstPlayerBetRounds = firstPlayerBetRounds;
		checkArgument(firstPlayerBetRounds >= 0
				&& firstPlayerBetRounds < params.getStacks().length,
				"Wrong first player %s", firstPlayerBetRounds);
		hasAnte = params.isEnableAnte();
		hasBlinds = params.isEnableBlinds();
		this.isCash = isCash;
		this.params = params;
		roundOffset = (hasAnte ? 1 : 0) + (hasBlinds ? 1 : 0);
		this.nbBetRounds = nbBetRounds;
		nbRounds = roundOffset + nbBetRounds;
		betRounds = new NLBetRound[nbBetRounds];
		if (hasAnte) {
			this.anteRound = new AnteRound(params);
			rType = RoundType.ANTE;
		} else if (hasBlinds) {
			this.blindsRound = new BlindsRound(params);
			rType = RoundType.BLINDS;
		} else {
			this.betRounds[0] = new NLBetRound(new BetRoundStartData(
					new PlayersData(params.getInHand(), params.getStacks(),
							new int[params.getStacks().length]),
					firstPlayerBetRounds, params.getBbValue()));
			rType = RoundType.BETS;
		}
		round = 0;
	}

	private boolean isAnteRound() {
		return hasAnte && round == 0;
	}

	private boolean isBlindsRound() {
		return hasBlinds
				&& ((hasAnte && round == 1) || (!hasAnte && round == 0));
	}

	private boolean isBetRound() {
		return round - roundOffset >= 0;
	}

	public int getBettingPlayer() {
		checkArgument(isBetRound(), "Doesn't seem to be in a bet round");
		checkArgument(
				betRounds[round - roundOffset].getState() == RoundState.WAITING_MOVE,
				"Bet round is in wrong state %s expected %s", betRounds[round
						- roundOffset].getState(), RoundState.WAITING_MOVE);
		return betRounds[round - roundOffset].getCurrentPlayer();
	}

	public BetChoice getBetChoice() {
		checkArgument(isBetRound(), "Doesn't seem to be in a bet round");
		checkArgument(
				betRounds[round - roundOffset].getState() == RoundState.WAITING_MOVE,
				"Bet round is in wrong state %s expected %s", betRounds[round
						- roundOffset].getState(), RoundState.WAITING_MOVE);
		return betRounds[round - roundOffset].getBetChoice();
	}

	public PlayersData getPlayersData() {
		switch (rType) {
		case ANTE:
			return anteRound.getData();
		case BETS:
			return betRounds[round - roundOffset].getData();
		case BLINDS:
			return blindsRound.getData();
		default:
			break;
		}
		return null;
	}

	public boolean doMove(Move<Integer> move) {
		if (isAnteRound())
			return doAnteMove(move);
		if (isBlindsRound())
			return doBlindsMove(move);
		if (isBetRound())
			return doBetMove(move);
		log.warn("Can't do move {}, seems like there's no hand going on", move);
		return false;
	}

	/**
	 * @param move
	 * @return
	 */
	private boolean doBetMove(Move<Integer> move) {
		switch (move.getType()) {
		case BET:
		case CALL:
		case FOLD:
		case RAISE:
			try {
				betRounds[round - roundOffset].doMove(move);
				return true;
			} catch (Exception e) {
				log.warn("Invalid bet-round move {}", move, e);
			}
			return false;
		default:
			log.warn("Invalid move in round {} ({}) : {}", round,
					getRoundType(), move);
			return false;
		}
	}

	/**
	 * @param move
	 * @return
	 */
	private boolean doBlindsMove(Move<Integer> move) {
		switch (move.getType()) {
		case NO_BLIND:
			if (!params.isCash()) {
				log.warn("Can't refuse to pay blinds in CG");
				return false;
			}
		case SB:
		case BB:
			try {
				blindsRound.doMove(move);
				return true;
			} catch (Exception e) {
				log.warn("Invalid blinds move {}", move, e);
			}
			return false;
		default:
			log.warn("Invalid move in round {} ({}) : {}", round,
					getRoundType(), move);
			return false;
		}
	}

	private boolean doAnteMove(Move<Integer> move) {
		switch (move.getType()) {
		case NO_ANTE:
			checkState(
					isCash,
					"Move %s is invalid because player can only refuse to pay ante in cash game",
					move);
		case ANTE:
			try {
				anteRound.doMove(move);
				return true;
			} catch (Exception e) {
				log.warn("Invalid ante move {}", move, e);
			}
			return false;
		default:
			log.warn("Invalid move in round {} ({}) : {}", round,
					getRoundType(), move);
			return false;
		}
	}

	public List<Move<Integer>> getAnteMoves() {
		return anteRound == null ? new LinkedList<Move<Integer>>() : anteRound
				.getMoves();
	}

	public List<Move<Integer>> getBlindsMoves() {
		return blindsRound == null ? new LinkedList<Move<Integer>>()
				: blindsRound.getMoves();
	}

	public List<Move<Integer>> getBetMoves(int betRoundIndex) {
		return (betRoundIndex < 0 || betRoundIndex >= nbBetRounds || betRounds[betRoundIndex] == null) ? new LinkedList<Move<Integer>>()
				: betRounds[betRoundIndex].getMoves();
	}

	public List<List<Move<Integer>>> getBetMoves() {
		List<List<Move<Integer>>> res = new LinkedList<>();
		List<Move<Integer>> list;
		for (int i = 0; i < nbBetRounds; i++) {
			if ((list = getBetMoves(i)).isEmpty())
				break;
			res.add(list);
		}
		return res;
	}

	public boolean allAntePayed() {
		if (!isAnteRound())
			return false;
		return anteRound.finished();
	}

	public Map<Integer, AnteValue> getMissingAnte() {
		if (!isAnteRound())
			return new HashMap<>();
		Map<Integer, AnteValue> res = new TreeMap<>();
		for (Integer p : anteRound.getMissingAntePlayers())
			res.put(p, anteRound.getAnteValueForPlayer(p));
		return res;
	}

	public Map<Integer, BlindValue> getMissingBlinds() {
		if (!isBlindsRound())
			return new HashMap<>();
		Map<Integer, BlindValue> res = new TreeMap<>();
		for (Integer p : blindsRound.getMissingEnteringBbPlayers())
			res.put(p, blindsRound.getBlindValueForPlayer(p));
		if (!res.containsKey(params.getBbIndex()) && !blindsRound.hasBbPayed())
			res.put(params.getBbIndex(),
					blindsRound.getBlindValueForPlayer(params.getBbIndex()));
		if (params.getSbIndex() >= 0 && !blindsRound.hasSbPayed())
			res.put(params.getSbIndex(),
					blindsRound.getBlindValueForPlayer(params.getSbIndex()));
		return res;
	}

	public boolean doAnteExpiration() {
		if (!isAnteRound()) {
			log.warn(
					"Not in ante round, current round type {}, round index {}",
					rType, round);
			return false;
		}
		anteRound.expiration();
		return true;
	}

	public boolean doBlindsExpiration() {
		if (!isBlindsRound()) {
			log.warn(
					"Not in ante round, current round type {}, round index {}",
					rType, round);
			return false;
		}
		blindsRound.expiration();
		return true;
	}

	public boolean nextRoundAfterAnte() {
		if (!isAnteRound()) {
			log.warn(
					"Not in ante round, current round type {}, round index {}",
					rType, round);
			return false;
		}
		if (anteRound.getState() != RoundState.NEXT_ROUND) {
			log.warn(
					"Wrong ante round state to go to next round {}, expected {}",
					anteRound.getState(), RoundState.NEXT_ROUND);
			return false;
		}
		if (hasBlinds) {
			BlindsAnteParameters initBlinds = new BlindsAnteParameters(
					anteRound.getData().getNoBetData(), this.params.getSpecs());
			try {
				blindsRound = new BlindsRound(initBlinds);
			} catch (Exception e) {
				log.warn("Can't start blinds round", e);
				return false;
			}
			round = 1;
			rType = RoundType.BLINDS;
			return true;
		}
		return nextBetRound(new BetRoundStartData(new PlayersData(anteRound
				.getData().getNoBetData()), firstPlayerBetRounds,
				params.getBbValue()));
	}

	public boolean betRoundAfterBlinds() {
		if (!isBlindsRound()) {
			log.warn(
					"Not in blinds round, current round type {}, round index {}",
					rType, round);
			return false;
		}
		if (blindsRound.getState() != RoundState.NEXT_ROUND) {
			log.warn(
					"Wrong blinds round state to go to bet round {}, expected {}",
					blindsRound.getState(), RoundState.NEXT_ROUND);
			return false;
		}
		return nextBetRound(new BetRoundStartData(blindsRound.getData(),
				params.getFirstPlayerAfterBlinds(), params.getBbValue()));
	}

	public boolean nextBetRound() {
		if (!isBetRound()) {
			log.warn(
					"Not in bets round, current round type {}, round index {}",
					rType, round);
			return false;
		}
		if (betRounds[round - roundOffset].getState() != RoundState.NEXT_ROUND) {
			log.warn(
					"Wrong bets round state to go to next bet round {}, expected {}",
					blindsRound.getState(), RoundState.NEXT_ROUND);
			return false;
		}
		if (round == nbRounds - 1) {
			log.warn(
					"There is no next bet round, current round index {}, bet round index {}",
					round, round - roundOffset);
			return false;
		}
		return nextBetRound(new BetRoundStartData(new PlayersData(
				betRounds[round - roundOffset].getData().getNoBetData()),
				firstPlayerBetRounds, params.getBbValue()));
	}

	private boolean nextBetRound(BetRoundStartData data) {
		try {
			betRounds[round + 1 - roundOffset] = new NLBetRound(data);
		} catch (Exception e) {
			log.warn("Can't start next bet round", e);
			return false;
		}
		round++;
		rType = RoundType.BETS;
		return true;
	}

	public RoundType getRoundType() {
		return rType;
	}

	public RoundState getRoundState() {
		if (isAnteRound())
			return anteRound.getState();
		if (isBlindsRound())
			return blindsRound.getState();
		if (round == nbRounds - 1
				&& betRounds[round - roundOffset].getState() == RoundState.NEXT_ROUND)
			return RoundState.SHOWDOWN;
		return betRounds[round - roundOffset].getState();
	}

	public int getRoundIndex() {
		return round;
	}

	public int getBetRoundIndex() {
		return round - roundOffset;
	}

	Optional<BetChoice> getPlayerBetChoice() {
		if (!isBetRound() || getRoundState() != RoundState.WAITING_MOVE)
			return Optional.absent();
		return Optional.of(betRounds[round - roundOffset].getBetChoice());
	}

	public List<Pot<Integer>> getCurrentPots() {
		List<Pot<Integer>> pots = new LinkedList<>();
		PlayersData data;
		if (hasAnte && round > 0) {
			data = anteRound.getData();
			pots.addAll(Pot.getPots(data.getBets(), data.getInHand()));
		}
		for (int i = 0; i <= round - roundOffset; i++) {
			switch (betRounds[i].getState()) {
			case CANCELED:
				log.warn("Last round was canceled");
				return pots;
			case END_NO_SHOWDOWN:
			case NEXT_ROUND:
			case SHOWDOWN:
				data = betRounds[i].getData();
				if (pots.isEmpty())
					pots.addAll(Pot.getPots(data.getBets(), data.getInHand()));
				else
					pots.addAll(Pot.getPots(pots.get(pots.size() - 1),
							data.getBets(), data.getInHand()));
				break;
			case WAITING_MOVE:
				// Don't add last round pots as it's not finished
				break;
			}
		}
		return pots;
	}

	public Optional<List<SharedPot<Integer>>> getSharedPots() {
		List<SharedPot<Integer>> res = new LinkedList<>();
		switch (getRoundState()) {
		case CANCELED:
			log.warn("Current round was canceled, can't share pots");
			return Optional.absent();
		case WAITING_MOVE:
			log.warn("Current round isn't finished, can't share pots");
			return Optional.absent();
		case SHOWDOWN:
			log.warn("For showdown, can't share pots without the ordered winners");
			return Optional.absent();
		case NEXT_ROUND:
			log.warn("Can't share pots, there is another round to play");
			return Optional.absent();
		case END_NO_SHOWDOWN:
			break;
		}
		PlayersData data = betRounds[round - roundOffset].getData();
		final int nbPlayers = data.getInHand().length;
		Integer winner = -1;
		for (int i = 0; i < nbPlayers; i++)
			if (data.getInHand()[i])
				if (winner >= 0)
					log.error(
							"There is more than one player in hand, this shouldn't happen when in state {}",
							getRoundState());
				else
					winner = i;
		List<Pot<Integer>> pots = getCurrentPots();
		List<Integer> winners = Lists.newArrayList(winner);
		for (int i = 0; i < pots.size(); i++)
			res.add(SharedPot.sharePot(pots.get(i), winners, winner));
		return Optional.of(res);
	}

	public Optional<List<SharedPot<Integer>>> getSharedPots(
			List<List<Integer>> orderedWinnersPartition) {

		switch (getRoundState()) {
		case CANCELED:
			log.warn("Current round was canceled, can't share pots");
			return Optional.absent();
		case WAITING_MOVE:
			log.warn("Current round isn't finished, can't share pots");
			return Optional.absent();
		case NEXT_ROUND:
			log.warn("Can't share pots, there is another round to play");
			return Optional.absent();
		case SHOWDOWN:
		case END_NO_SHOWDOWN:
			break;
		}
		List<List<Integer>> filteredWinners = new LinkedList<>();
		boolean[] inHand = betRounds[round - roundOffset].getData().getInHand();
		for (List<Integer> list : orderedWinnersPartition)
			for (Integer p : list)
				if (p >= inHand.length || p < 0) {
					log.error(
							"Invalid player integer in winners partition : {}",
							p);
					return Optional.absent();
				}
		for (int i = 0; i < orderedWinnersPartition.size(); i++) {
			List<Integer> winners = new LinkedList<>();
			for (Integer p : orderedWinnersPartition.get(i))
				if (inHand[p])
					winners.add(p);
			if (!winners.isEmpty())
				filteredWinners.add(winners);
		}
		if (filteredWinners.isEmpty()) {
			log.error(
					"Can't find winners from the provided players partition {} that are in hand {}",
					orderedWinnersPartition, inHand);
			return Optional.absent();
		}
		missingPlayerLoop: for (int i = 0; i < inHand.length; i++)
			if (inHand[i]) {
				for (List<Integer> winners : filteredWinners)
					if (winners.contains(i))
						continue missingPlayerLoop;
				log.error(
						"Can't find in hand player {} from the provided players partition {}",
						i, orderedWinnersPartition);
				return Optional.absent();
			}
		List<SharedPot<Integer>> res = new LinkedList<>();
		potsLoop: for (Pot<Integer> pot : getCurrentPots()) {
			for (List<Integer> winners : filteredWinners)
				if (joinNotEmpty(winners, pot.getPlayers())) {
					// TODO odd chips winner explicit declaration ??
					res.add(SharedPot.sharePot(pot, winners, winners.get(0)));
					continue potsLoop;
				}
			log.error(
					"Didn't find winners for pot {} with provided winners partition {}, filtered winners {}",
					pot, orderedWinnersPartition, filteredWinners);
			return Optional.absent();
		}
		return Optional.of(res);
	}

	private static <Id> boolean joinNotEmpty(List<Id> list1, List<Id> list2) {
		for (Id i : list1)
			if (list2.contains(i))
				return true;
		return false;
	}

	@Override
	public NLHandRounds clone() {
		return new NLHandRounds(this);
	}
}

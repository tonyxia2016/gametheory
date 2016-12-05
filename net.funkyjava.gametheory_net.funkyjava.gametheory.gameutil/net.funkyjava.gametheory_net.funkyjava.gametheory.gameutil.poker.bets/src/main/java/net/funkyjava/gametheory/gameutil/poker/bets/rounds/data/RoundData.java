/**
 * 
 */
package net.funkyjava.gametheory.gameutil.poker.bets.rounds.data;

import lombok.AllArgsConstructor;
import lombok.Delegate;
import lombok.Getter;
import lombok.NonNull;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BlindsAnteSpec;

/**
 * @author Pierre Mardon
 * 
 */
@AllArgsConstructor
public class RoundData {

	@Getter
	@NonNull
	@Delegate
	private final PlayerData playersData;
	@Getter
	@NonNull
	@Delegate
	private final BlindsAnteSpec specs;

}

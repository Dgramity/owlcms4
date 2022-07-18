/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class CurrentAthlete extends PolymerElement {
	static get is() {
		return 'currentathlete-template'
	}

	static get template() {
		return html`
<link rel="stylesheet" type="text/css" href="local/styles/currentathlete.css">
<div class$="wrapper [[teamWidthClass]] [[inactiveClass]]">

	<!-- this div is SHOWN when the platform is inactive -->
	<div style$="[[inactiveStyle]]">
		<div class="competitionName">[[competitionName]]</div><br>
		<div class="nextGroup">[[t.WaitingNextGroup]]</div>
	</div>

	<!-- the remaining divs are HIDDEN when the platform is inactive -->
	<div class="attemptBar" style$="[[hiddenStyle]]; display: grid;">
		<div class="startNumber" id="startNumberDiv"><span>[[startNumber]]</span></div>
		<div class="fullName ellipsis" id="fullNameDiv" inner-h-t-m-l="[[fullName]]">[[fullName]]</div>
		<div class="clubName ellipsis" id="teamNameDiv">
			<div class="clubNameEllipsis">[[teamName]]</div>
		</div>
		<div class="attempt" id="attemptDiv"><span inner-h-t-m-l="[[attempt]]"></span></div>
		<div class="weight" id="weightDiv">
			<span>[[weight]]<span style="font-size: 75%">&nbsp;[[t.KgSymbol]]</span></span>
		</div>
		<div class="timer athleteTimer" id="timerDiv">
			<timer-element id="timer"></timer-element>
		</div>
		<div class="timer breakTime" id="breakTimerDiv">
			<timer-element id="breakTimer"></timer-element>
		</div>
		<div class="decisionBox" id="decisionDiv">
			<decision-element id="decisions"></decision-element>
		</div>
		<div class="attempts">
			<table class="results" style$="[[hiddenStyle]]">
				<template is="dom-repeat" id="result-table" items="[[athletes]]" as="l">
					<template is="dom-if" if="[[!l.isSpacer]]">
						<tr>
							<td class="category">
								<div>[[l.category]]</div>
							</td>
							<td class="liftName"><div inner-h-t-m-l="[[t.Snatch]]"></div></td>
							<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
								<td class$="[[attempt.goodBadClassName]]">
									<div>[[attempt.stringValue]]</div>
								</td>
							</template>
							<td class="showRank">
								<div>[[t.Rank]] <b>[[l.snatchRank]]</b></div>
							</td>
							<td class="liftName"><div inner-h-t-m-l="[[t.Clean_and_Jerk]]"></div></td>
							<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
								<td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
									<div>[[attempt.stringValue]]</div>
								</td>
							</template>
							<td class="showRank">
								<div><div>[[t.Rank]] <b>[[l.cleanJerkRank]]</b></div></div>
							</td>
							<td class="liftName"><div inner-h-t-m-l="[[t.Total]]"></div></td>
							<td class="total">[[l.total]]</td>
							<td class="totalRank">
								<div>[[t.Rank]] <b>[[l.totalRank]]</b></div>
							</td>
						</tr>
					</template>
				</template>
			</table>
		</div>
	</div>
</div>`;
	}

	ready() {
		console.debug("ready");
		super.ready();
		document.body.setAttribute("theme","dark");
		this.$.fullNameDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.display = "grid";
		this.$.startNumberDiv.style.display = "grid";
		this.$.teamNameDiv.style.display = "grid";
		this.$.attemptDiv.style.display = "grid";
		this.$.weightDiv.style.display = "grid";
		this.$.timerDiv.style.display = "grid";
		this.$.breakTimerDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
	}

	start() {
		this.$.timer.start();
	}

	reset() {
		console.debug("reset");
		this.$.timer.reset(this.$.timer);
		this.$.fullNameDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.display = "grid";
		this.$.startNumberDiv.style.display = "grid";
		this.$.teamNameDiv.style.display = "grid";
		this.$.attemptDiv.style.display = "grid";
		this.$.weightDiv.style.display = "grid";
		this.$.timerDiv.style.display = "grid";
		this.$.breakTimerDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
	}

	down() {
		console.debug("refereeDecision");
		this.$.startNumberDiv.style.display = "grid";
		this.$.teamNameDiv.style.display = "grid";
		this.$.attemptDiv.style.display = "grid";
		this.$.weightDiv.style.display = "grid";
		this.$.timerDiv.style.display = "grid";
		this.$.breakTimerDiv.style.display = "none";
		this.$.decisionDiv.style.display = "grid";
	}

	doBreak() {
		console.debug("break");
		this.$.fullNameDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.display = "grid";
		this.$.startNumberDiv.style.display = "none";
		this.$.teamNameDiv.style.display = "none";
		this.$.attemptDiv.style.display = "none";
		this.$.weightDiv.style.display = "none";
		this.$.timerDiv.style.display = "none";
		this.$.breakTimerDiv.style.display = "grid";
		this.$.decisionDiv.style.display = "none";
	}

	groupDone() {
		console.debug("done");
		this.$.fullNameDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.display = "grid";
		this.$.startNumberDiv.style.display = "none";
		this.$.teamNameDiv.style.display = "none";
		this.$.attemptDiv.style.display = "none";
		this.$.weightDiv.style.display = "none";
		this.$.timerDiv.style.display = "none";
		this.$.breakTimerDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
	}

	refereeDecision() {
		console.debug("refereeDecision");
		this.$.decisionDiv.style.display = "grid";
		this.$.weightDiv.style.display = "grid";
		this.$.timerDiv.style.display = "grid";
		this.$.breakTimerDiv.style.display = "none";
	}

	_isEqualTo(title, string) {
		return title == string;
	}
}

customElements.define(CurrentAthlete.is, CurrentAthlete);

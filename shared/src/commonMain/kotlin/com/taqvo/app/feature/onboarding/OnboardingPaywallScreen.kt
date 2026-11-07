package com.taqvo.app.feature.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.components.CheckMark
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.design.components.drawClose
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.onb_paywall_benefit_1
import com.taqvo.app.resources.onb_paywall_benefit_1_body
import com.taqvo.app.resources.onb_paywall_benefit_2
import com.taqvo.app.resources.onb_paywall_benefit_2_body
import com.taqvo.app.resources.onb_paywall_benefit_3
import com.taqvo.app.resources.onb_paywall_benefit_3_body
import com.taqvo.app.resources.onb_paywall_benefit_4
import com.taqvo.app.resources.onb_paywall_benefit_4_body
import com.taqvo.app.resources.onb_paywall_cancel_anytime
import com.taqvo.app.resources.onb_paywall_close
import com.taqvo.app.resources.onb_paywall_discount
import com.taqvo.app.resources.onb_paywall_monthly
import com.taqvo.app.resources.onb_paywall_monthly_summary
import com.taqvo.app.resources.onb_paywall_no_charge_now
import com.taqvo.app.resources.onb_paywall_per_month
import com.taqvo.app.resources.onb_paywall_privacy
import com.taqvo.app.resources.onb_paywall_restore
import com.taqvo.app.resources.onb_paywall_subscribe
import com.taqvo.app.resources.onb_paywall_terms
import com.taqvo.app.resources.onb_paywall_timeline_charge
import com.taqvo.app.resources.onb_paywall_timeline_charge_body
import com.taqvo.app.resources.onb_paywall_timeline_reminder
import com.taqvo.app.resources.onb_paywall_timeline_reminder_body
import com.taqvo.app.resources.onb_paywall_timeline_today
import com.taqvo.app.resources.onb_paywall_timeline_today_body
import com.taqvo.app.resources.onb_paywall_trial_cta
import com.taqvo.app.resources.onb_paywall_trial_label
import com.taqvo.app.resources.onb_paywall_trial_title
import com.taqvo.app.resources.onb_paywall_yearly
import com.taqvo.app.resources.onb_paywall_yearly_summary
import com.taqvo.app.resources.onb_paywall_yearly_title

enum class Plan { MONTHLY, YEARLY }

/**
 * The two plans as the store prices them (spec §15). Every price is the store's own localised
 * string; nothing here is ever written in by hand. [trialEligible] is the store's answer, not
 * the app's guess: a reader who has had a trial before is shown the plan without one.
 */
data class PaywallOffer(
    val yearlyPrice: String,
    val yearlyPerMonth: String,
    val monthlyPrice: String,
    /** The yearly saving against twelve months, from the store's prices ([discountPercent]). */
    val discountPercent: Int,
    val trialDays: Int,
    val trialEligible: Boolean,
    /** The day the first charge falls on if the trial is not cancelled, already formatted. */
    val chargeDate: String,
)

/** The yearly plan's saving against twelve monthly payments, rounded down; 0 if it saves nothing. */
fun discountPercent(yearlyMicros: Long, monthlyMicros: Long): Int {
    if (monthlyMicros <= 0) return 0
    val twelve = monthlyMicros * 12
    return ((twelve - yearlyMicros) * 100 / twelve).toInt().coerceAtLeast(0)
}

/**
 * Screen 15, built but switched off ([OnboardingFeatures.PAYWALL_ENABLED]). The two plans each
 * have their own layout: the yearly plan lists what it unlocks, the monthly one the trial's
 * timeline. The close mark is small and always there (the soft paywall the spec observed).
 */
@Composable
internal fun PaywallScreen(
    offer: PaywallOffer,
    plan: Plan,
    onPlan: (Plan) -> Unit,
    onPurchase: () -> Unit,
    onClose: () -> Unit,
    onRestore: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
) {
    val colors = LocalTaqvoColors.current
    val trial = plan == Plan.MONTHLY && offer.trialEligible
    OnbPage(
        progress = null,
        onSkip = null,
        centred = false,
        content = {
            val closeLabel = stringResource(Res.string.onb_paywall_close)
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onClose)
                        .semantics { contentDescription = closeLabel },
                    contentAlignment = Alignment.Center,
                ) { Canvas(Modifier.size(14.dp)) { drawClose(colors.textTertiary) } }
            }
            Crossfade(trial, animationSpec = tween(200), label = "paywall") { showTrial ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showTrial) {
                        OnbTitle(stringResource(Res.string.onb_paywall_trial_title, LocalPlatformFormat.current.localizedDigits(offer.trialDays)))
                        Spacer(Modifier.height(20.dp))
                        Timeline(offer)
                    } else {
                        OnbTitle(stringResource(Res.string.onb_paywall_yearly_title))
                        Spacer(Modifier.height(20.dp))
                        Benefits()
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            PlanCard(
                selected = plan == Plan.YEARLY,
                badge = stringResource(Res.string.onb_paywall_discount, LocalPlatformFormat.current.localizedDigits(offer.discountPercent)).takeIf { offer.discountPercent > 0 },
                top = null,
                title = stringResource(Res.string.onb_paywall_yearly),
                price = stringResource(Res.string.onb_paywall_per_month, offer.yearlyPerMonth),
                onClick = { onPlan(Plan.YEARLY) },
            )
            Spacer(Modifier.height(10.dp))
            PlanCard(
                selected = plan == Plan.MONTHLY,
                badge = null,
                top = stringResource(Res.string.onb_paywall_trial_label, LocalPlatformFormat.current.localizedDigits(offer.trialDays)).takeIf { offer.trialEligible },
                title = stringResource(Res.string.onb_paywall_monthly),
                price = stringResource(Res.string.onb_paywall_per_month, offer.monthlyPrice),
                onClick = { onPlan(Plan.MONTHLY) },
            )
        },
        footer = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckMark(Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(if (trial) Res.string.onb_paywall_no_charge_now else Res.string.onb_paywall_cancel_anytime),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            TaqvoPrimaryButton(
                stringResource(if (trial) Res.string.onb_paywall_trial_cta else Res.string.onb_paywall_subscribe, LocalPlatformFormat.current.localizedDigits(offer.trialDays)),
                onPurchase,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (plan == Plan.YEARLY) {
                    stringResource(Res.string.onb_paywall_yearly_summary, offer.yearlyPrice, offer.yearlyPerMonth)
                } else {
                    stringResource(Res.string.onb_paywall_monthly_summary, offer.monthlyPrice)
                },
                color = colors.textTertiary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FooterLink(stringResource(Res.string.onb_paywall_restore), onRestore)
                FooterLink(stringResource(Res.string.onb_paywall_privacy), onPrivacy)
                FooterLink(stringResource(Res.string.onb_paywall_terms), onTerms)
            }
        },
    )
}

@Composable
private fun Benefits() {
    val colors = LocalTaqvoColors.current
    val items = listOf(
        Res.string.onb_paywall_benefit_1 to Res.string.onb_paywall_benefit_1_body,
        Res.string.onb_paywall_benefit_2 to Res.string.onb_paywall_benefit_2_body,
        Res.string.onb_paywall_benefit_3 to Res.string.onb_paywall_benefit_3_body,
        Res.string.onb_paywall_benefit_4 to Res.string.onb_paywall_benefit_4_body,
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEach { (head, body) ->
            Row {
                CheckMark(Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(head), color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stringResource(body), color = colors.textSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

/** The trial's three days: today, the reminder, the first charge. The first node is lit. */
@Composable
private fun Timeline(offer: PaywallOffer) {
    val colors = LocalTaqvoColors.current
    val format = LocalPlatformFormat.current
    val nodes = listOf(
        stringResource(Res.string.onb_paywall_timeline_today) to stringResource(Res.string.onb_paywall_timeline_today_body),
        stringResource(Res.string.onb_paywall_timeline_reminder, format.localizedDigits(offer.trialDays - 1)) to
            stringResource(Res.string.onb_paywall_timeline_reminder_body),
        stringResource(Res.string.onb_paywall_timeline_charge, format.localizedDigits(offer.trialDays)) to
            stringResource(Res.string.onb_paywall_timeline_charge_body, offer.chargeDate),
    )
    Column(Modifier.fillMaxWidth()) {
        nodes.forEachIndexed { index, (head, body) ->
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(if (index == 0) colors.accent else colors.hairline))
                    if (index < nodes.lastIndex) Box(Modifier.width(2.dp).height(34.dp).background(colors.hairline))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.offset(y = 2.dp)) {
                    Text(head, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(body, color = colors.textSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PlanCard(selected: Boolean, badge: String?, top: String?, title: String, price: String, onClick: () -> Unit) {
    val colors = LocalTaqvoColors.current
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = if (badge != null) 10.dp else 0.dp)
                .clip(shape)
                .background(colors.surface)
                .border(if (selected) 2.dp else 1.dp, if (selected) colors.accent else colors.hairline, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (top != null) Text(top, color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(title, color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Text(price, color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (badge != null) {
            Text(
                badge,
                color = colors.background,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(50))
                    .background(colors.accent)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = LocalTaqvoColors.current.textTertiary,
        fontSize = 12.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onClick).padding(8.dp),
    )
}

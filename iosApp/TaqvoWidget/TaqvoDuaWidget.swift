import WidgetKit
import SwiftUI
import widgetcore

// The daily-dua card, the ayah card's companion: one of the adhkar's supplications said at any
// hour, its translation in the interface language, and its number in Hisn al-Muslim. The app
// writes the pool (`DuaPoolMirror`) beside the ayah pool and reloads every timeline when it does;
// the rotation is the ayah card's own, so both cards turn over at the same local midnight.

struct DuaEntry: TimelineEntry {
    let date: Date
    /// Today's supplication, or nil before the app has ever written the dua mirror.
    let entry: DuaPoolEntry?
    let translationRtl: Bool
    let languageTag: String
    let arabicIndicDigits: Bool
    let background: WidgetBackground
}

struct DuaTimelineProvider: TimelineProvider {
    private static let futureDays = 6

    func placeholder(in context: Context) -> DuaEntry {
        DuaEntry(date: Date(), entry: nil, translationRtl: false, languageTag: "", arabicIndicDigits: false, background: .followTheme)
    }

    func getSnapshot(in context: Context, completion: @escaping (DuaEntry) -> Void) {
        let entry = Self.entries(from: Date()).first!
        // The gallery may ask before the app has run once: show a real supplication there.
        if context.isPreview && entry.entry == nil {
            completion(Self.sampleEntry(background: entry.background))
        } else {
            completion(entry)
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<DuaEntry>) -> Void) {
        completion(Timeline(entries: Self.entries(from: Date()), policy: .atEnd))
    }

    static func entries(from start: Date) -> [DuaEntry] {
        let store = KeyValueStore_iosKt.createWidgetKeyValueStore()
        let mirror = DuaPoolMirror.companion.read(store: store)
        let seed = AyahPoolMirror.companion.seed(store: store)?.int64Value ?? 0
        let background = TaqvoMirror.background()
        return dates(from: start).map { date in
            DuaEntry(
                date: date,
                entry: mirror?.entryFor(epochDay: epochDay(of: date), ayahSeed: seed),
                translationRtl: mirror?.translationRtl ?? false,
                languageTag: mirror?.languageTag ?? "",
                arabicIndicDigits: mirror?.arabicIndicDigits ?? false,
                background: background
            )
        }
    }

    /// Gregorian whatever the phone's calendar, in the phone's zone: the ayah card's rule, so the
    /// two cards agree on the date (see `AyahTimelineProvider.rotationCalendar`).
    private static var rotationCalendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = Calendar.current.timeZone
        return cal
    }

    private static func dates(from start: Date) -> [Date] {
        let calendar = rotationCalendar
        var dates = [start]
        var day = calendar.startOfDay(for: start)
        for _ in 0..<futureDays {
            guard let next = calendar.date(byAdding: .day, value: 1, to: day) else { break }
            day = calendar.startOfDay(for: next)
            dates.append(day)
        }
        return dates
    }

    private static func epochDay(of date: Date) -> Int64 {
        let c = rotationCalendar.dateComponents([.year, .month, .day], from: date)
        return AyahRotation.shared.epochDay(
            year: Int32(c.year ?? 1970), month: Int32(c.month ?? 1), day: Int32(c.day ?? 1)
        )
    }

    /// Hisn al-Muslim 91, for the gallery alone.
    static func sampleEntry(background: WidgetBackground) -> DuaEntry {
        let tag = Locale.current.language.languageCode?.identifier ?? "en"
        let entry = DuaPoolEntry(
            hisn: 91,
            arabic: "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            translation: tag.hasPrefix("ar") ? "" : "Glory is to Allah and praise is to Him."
        )
        return DuaEntry(
            date: Date(), entry: entry, translationRtl: false, languageTag: tag,
            arabicIndicDigits: WidgetDigits.shared.defaultsToArabicIndic(languageTag: tag),
            background: background
        )
    }
}

struct TaqvoDuaWidgetView: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var colorScheme
    let entry: DuaEntry

    private var colors: WidgetPaletteColors {
        WidgetPalette.shared.colorsFor(background: entry.background, systemIsDark: colorScheme == .dark)
    }
    private var textColor: Color { Color(argb: colors.textArgb) }
    private var accentColor: Color { Color(argb: colors.accentArgb) }
    private var large: Bool { family == .systemLarge }

    var body: some View {
        Group {
            if let dua = entry.entry {
                card(dua)
            } else {
                Text(NSLocalizedString("dua_widget_placeholder", comment: ""))
                    .font(.system(size: 14))
                    .multilineTextAlignment(.center)
                    .foregroundColor(textColor.opacity(0.62))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .padding(TaqvoWidgetSurface.cardPadding(inWidgetContainer: true))
        .taqvoSurface(colors, frosted: entry.background == .translucentOrFrosted, inWidgetContainer: true)
    }

    private func card(_ dua: DuaPoolEntry) -> some View {
        let number = WidgetDigits.shared.localize(
            text: "\(dua.hisn)", nativeDigits: entry.arabicIndicDigits, languageTag: entry.languageTag
        )
        let reference = String(format: NSLocalizedString("dua_widget_reference", comment: ""), number)
        return VStack(spacing: 8) {
            Spacer(minLength: 0)
            // The OS face, as the adhkar page sets a supplication; it shrinks rather than cuts.
            Text(dua.arabic)
                .font(.system(size: large ? 26 : 20))
                .foregroundColor(textColor)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.55)
                .environment(\.layoutDirection, .rightToLeft)
            if !dua.translation.isEmpty {
                Text(dua.translation)
                    .font(.system(size: large ? 14 : 12))
                    .foregroundColor(textColor.opacity(0.62))
                    .multilineTextAlignment(.center)
                    .lineLimit(large ? 6 : 2)
                    .minimumScaleFactor(0.8)
                    .environment(\.layoutDirection, entry.translationRtl ? .rightToLeft : .leftToRight)
            }
            Spacer(minLength: 0)
            Text(reference)
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(accentColor)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // One stop: the reference, then the translation. A supplication is not the Quran, so the
        // Arabic may be read too, but VoiceOver's Arabic voice is often absent; the meaning comes first.
        .accessibilityElement(children: .ignore)
        .accessibilityLabel([NSLocalizedString("dua_widget_name", comment: ""), reference, dua.translation]
            .filter { !$0.isEmpty }.joined(separator: ". "))
    }
}

struct TaqvoDuaWidget: Widget {
    let kind = "TaqvoDuaWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: DuaTimelineProvider()) { entry in
            TaqvoDuaWidgetView(entry: entry)
        }
        .configurationDisplayName(LocalizedStringKey("dua_widget_name"))
        .description(LocalizedStringKey("dua_widget_description"))
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

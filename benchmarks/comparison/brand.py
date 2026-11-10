# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Shared light-theme chart system using the elide.dev typefaces and colors.

Fonts are fetched from pinned upstream commits, verified against fonts.json and
cached under build/fonts. SVG text is emitted as glyph outlines so README
renderers that cannot load web fonts still show the brand typefaces.
"""
import hashlib
import json
from pathlib import Path
import statistics
import urllib.request

import matplotlib
matplotlib.use('Agg')
from matplotlib import font_manager
from matplotlib.patches import FancyBboxPatch, Rectangle
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[2]
FONTS = Path(__file__).with_name('fonts.json')

# elide.dev light-theme tokens (css/tokens.css), converted from OKLCH.
SURFACE = '#fbfbfc'
TRACK = '#f3f3f5'
BORDER = '#e4e4e7'
GRID = '#ebebee'
TEXT = '#111114'
MUTED = '#4d4d50'
FAINT = '#76767c'
BRAND_TEXT = '#ad0071'
# Series roles, validated for CVD separation and 3:1 contrast on SURFACE.
MYNA = '#d4007f'
BASELINE = '#6f3fb3'
REFERENCE = '#a1a1aa'
SANS = 'Instrument Sans'
MONO = 'JetBrains Mono'
PT = 1 / 72


def fonts():
    cache = ROOT / 'build/fonts'
    cache.mkdir(parents=True, exist_ok=True)
    for name, pin in json.loads(FONTS.read_text()).items():
        path = cache / name
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != pin['sha256']:
            with urllib.request.urlopen(pin['url'], timeout=60) as response:
                payload = response.read()
            if hashlib.sha256(payload).hexdigest() != pin['sha256']:
                raise SystemExit(f'Font checksum mismatch: {name}')
            path.write_bytes(payload)
        font_manager.fontManager.addfont(str(path))
    plt.rcParams.update({'font.family': SANS, 'font.size': 11, 'svg.fonttype': 'path',
                         'axes.unicode_minus': False, 'text.color': TEXT})


def process_medians(results, key=lambda r: (r['mode'], r['case'])):
    groups = {}
    for r in results:
        groups.setdefault(key(r), []).append(statistics.median(s['ns'] / s['iterations'] for s in r['samples']))
    return groups


def fmt(value):
    return f'{value:.3f}' if value < 10 else f'{value:.2f}' if value < 100 else f'{value:.1f}'


class RangeChart:
    """Horizontal bars to the median of process medians, whiskers over their min-max.

    Layout is specified in points so marks keep fixed physical sizes: 14pt bars,
    4pt rounded data ends square at the baseline, and a 1pt hairline grid.
    """

    WIDTH = 12.0
    RIGHT = 1.05
    PAD = .5
    HEAD = 30
    GAP = 18
    BAR = 14

    def __init__(self, eyebrow, title, subtitle, groups, xmax, xticks, notes, provenance, unit='ns',
                 hero=None, legend=None, salt='myna', pitch=38, label_width=3.35, compact=False):
        # Compact groups put the group label beside unlabeled rows; the legend names the series.
        self.PITCH, self.LEFT = pitch, label_width
        self.HEAD, self.GAP = (0, 26) if compact else (self.HEAD, self.GAP)
        self.__dict__.update(locals())
        plt.rcParams['svg.hashsalt'] = salt

    def render(self, output):
        header = 100 + (30 if self.legend else 0)
        rows = sum(self.HEAD + self.PITCH * len(g['rows']) for g in self.groups) + self.GAP * (len(self.groups) - 1)
        footer = 56 + 15 * len(self.notes)
        height = (header + rows + 28 + footer) * PT + .2
        fig = plt.figure(figsize=(self.WIDTH, height))
        fig.patch.set_alpha(0)
        fig.add_artist(FancyBboxPatch((.06 / self.WIDTH, .06 / height), 1 - .12 / self.WIDTH, 1 - .12 / height,
                                      boxstyle=f'round,pad=0,rounding_size={14 * PT / self.WIDTH}',
                                      mutation_aspect=self.WIDTH / height, transform=fig.transFigure,
                                      facecolor=SURFACE, edgecolor=BORDER, linewidth=1, zorder=-10))
        top = height - .1 - (header * PT)
        bottom = top - rows * PT
        width = self.WIDTH - self.LEFT - self.RIGHT
        ax = fig.add_axes((self.LEFT / self.WIDTH, bottom / height, width / self.WIDTH, rows * PT / height))
        ax.set_xlim(0, self.xmax)
        ax.set_ylim(rows, 0)
        ax.patch.set_alpha(0)
        ax.spines[:].set_visible(False)
        ax.set_yticks([])
        ax.set_xticks(self.xticks)
        ax.set_xticklabels([f'{t:g}' for t in self.xticks[:-1]] + [f'{self.xticks[-1]:g} {self.unit}'],
                           fontfamily=MONO, fontsize=9, color=FAINT)
        ax.tick_params(axis='x', length=0, pad=8)
        x_per_in = self.xmax / width
        label_x = -(self.LEFT - self.PAD) / width
        y = 0
        for index, group in enumerate(self.groups):
            if index:
                y += self.GAP
                if self.compact:
                    ax.plot([label_x, 1 + (self.RIGHT - self.PAD) / width], [y - self.GAP / 2] * 2, color=GRID,
                            linewidth=1, transform=ax.get_yaxis_transform(), clip_on=False)
            if self.compact:
                middle = y + self.PITCH * len(group['rows']) / 2
                ax.text(label_x, middle - 1, group['title'], transform=ax.get_yaxis_transform(), fontsize=12,
                        fontweight='semibold', color=TEXT, va='bottom')
                ax.text(label_x, middle + 3, group.get('subtitle', ''), transform=ax.get_yaxis_transform(),
                        fontsize=8.5, fontfamily=MONO, color=FAINT, va='top')
            else:
                ax.text(label_x, y + 18, group['title'], transform=ax.get_yaxis_transform(), fontsize=11.5,
                        fontweight='semibold', color=TEXT, va='baseline')
                if group.get('subtitle'):
                    ax.text(1, y + 18, group['subtitle'], transform=ax.get_yaxis_transform(), fontsize=8.5,
                            fontfamily=MONO, color=FAINT, va='baseline', ha='right')
            y += self.HEAD
            for tick in self.xticks:
                ax.plot([tick, tick], [y, y + self.PITCH * len(group['rows'])], color=GRID, linewidth=1,
                        zorder=0, solid_capstyle='butt')
            for row in group['rows']:
                self._row(ax, row, y + self.PITCH / 2, x_per_in, label_x)
                y += self.PITCH
        self._header(fig, height)
        self._footer(fig, bottom - 28 * PT, height)
        output.parent.mkdir(parents=True, exist_ok=True)
        svg = output.with_suffix('.svg')
        fig.savefig(svg, metadata={'Date': None}, facecolor='none')
        fig.savefig(output.with_suffix('.png'), dpi=200, facecolor='none',
                    metadata={'Software': 'Myna chart system'})
        plt.close(fig)
        svg.write_text('\n'.join(line.rstrip() for line in svg.read_text().splitlines()) + '\n')
        return [svg, output.with_suffix('.png')]

    def _row(self, ax, row, center, x_per_in, label_x):
        values = row['values']
        median = statistics.median(values)
        bar = self.BAR
        r_x = 4 * PT * x_per_in
        ax.add_patch(FancyBboxPatch((0, center - bar / 2), median, bar,
                                    boxstyle=f'round,pad=0,rounding_size={r_x}', mutation_aspect=4 / r_x,
                                    facecolor=row['color'], linewidth=0, zorder=2))
        ax.add_patch(Rectangle((0, center - bar / 2), min(median / 2, r_x * 1.5), bar,
                               facecolor=row['color'], linewidth=0, zorder=2))
        low, high = min(values), max(values)
        whisker = dict(color=TEXT, linewidth=1.25, solid_capstyle='butt', zorder=4)
        ax.plot([low, high], [center, center], **whisker)
        ax.plot([low, low], [center - 4.5, center + 4.5], **whisker)
        ax.plot([high, high], [center - 4.5, center + 4.5], **whisker)
        ax.plot([median], [center], marker='o', markersize=6.5, markerfacecolor=row['color'],
                markeredgecolor=SURFACE, markeredgewidth=2, zorder=5)
        weight = 'semibold' if row.get('emphasis') else 'medium'
        ax.text(max(high, median) + 7 * PT * x_per_in, center, f"{fmt(median)} ns", fontfamily=MONO,
                fontsize=10.5, fontweight=weight, color=TEXT, va='center', zorder=6)
        transform = ax.get_yaxis_transform()
        if not row.get('label'):
            return
        ax.add_patch(Rectangle((label_x, center - 10), 3 * PT / (ax.get_position().width * self.WIDTH), 20,
                               transform=transform, facecolor=row['color'], linewidth=0, clip_on=False))
        indent = label_x + 11 * PT / (ax.get_position().width * self.WIDTH)
        if row.get('sublabel'):
            ax.text(indent, center - 1, row['label'], transform=transform, fontsize=12,
                    fontweight='semibold' if row.get('emphasis') else 'medium', color=TEXT, va='bottom')
            ax.text(indent, center + 2, row['sublabel'], transform=transform, fontsize=8.5,
                    fontfamily=MONO, color=FAINT, va='top')
        else:
            ax.text(indent, center, row['label'], transform=transform, fontsize=12,
                    fontweight='semibold' if row.get('emphasis') else 'medium', color=TEXT, va='center')

    def _header(self, fig, height):
        x = self.PAD / self.WIDTH
        y = lambda pts: (height - .1 - pts * PT) / height
        fig.text(x, y(32), self.eyebrow.upper(), fontfamily=MONO, fontsize=9, fontweight='semibold',
                 color=BRAND_TEXT, va='baseline')
        fig.text(x, y(62), self.title, fontsize=25, fontweight='semibold', color=TEXT, va='baseline')
        fig.text(x, y(84), self.subtitle, fontsize=12, color=MUTED, va='baseline')
        if self.hero:
            right = 1 - self.PAD / self.WIDTH
            fig.text(right, y(62), self.hero[0], fontsize=36, fontweight='semibold', color=TEXT,
                     ha='right', va='baseline')
            fig.text(right, y(84), self.hero[1], fontsize=10, color=MUTED, ha='right', va='baseline')
        if self.legend:
            cursor = x
            for label, color in self.legend:
                fig.add_artist(FancyBboxPatch((cursor, y(118)), 10 * PT / self.WIDTH, 10 * PT / height,
                                              boxstyle=f'round,pad=0,rounding_size={2.5 * PT / self.WIDTH}',
                                              mutation_aspect=self.WIDTH / height, transform=fig.transFigure,
                                              facecolor=color, linewidth=0))
                text = fig.text(cursor + 16 * PT / self.WIDTH, y(117), label, fontsize=10.5, color=TEXT,
                                va='baseline', fontweight='medium')
                cursor += (16 + 6.1 * len(label) + 24) * PT / self.WIDTH

    def _footer(self, fig, top, height):
        x = self.PAD / self.WIDTH
        right = 1 - self.PAD / self.WIDTH
        y = (top - 22 * PT) / height
        fig.add_artist(plt.Line2D([x, right], [y + 12 * PT / height] * 2, color=BORDER, linewidth=1,
                                  transform=fig.transFigure))
        for note in self.notes:
            fig.text(x, y - 6 * PT / height, note, fontsize=9.5, color=MUTED, va='baseline')
            y -= 15 * PT / height
        y -= 10 * PT / height
        fig.text(x, y, self.provenance, fontfamily=MONO, fontsize=8, color=FAINT, va='baseline')
        fig.text(right, y, 'github.com/elide-dev/myna', fontfamily=MONO, fontsize=8, fontweight='medium',
                 color=BRAND_TEXT, ha='right', va='baseline')


def manifest(output, inputs, script):
    files = [*inputs, Path(script), Path(__file__), FONTS, Path(__file__).with_name('requirements.txt'),
             output.with_suffix('.svg'), output.with_suffix('.png')]
    digests = {str(p.resolve().relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
    output.with_suffix('.manifest.json').write_text(json.dumps(digests, indent=2) + '\n')

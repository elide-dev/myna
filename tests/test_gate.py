# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Performance gates fail on consistent regressions and tolerate outlier processes."""
import contextlib
import importlib.util
import io
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('gate', Path(__file__).parents[1] / 'benchmarks/gate.py')
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)


def results(cases):
    """One sample per process, so each process median is the given value."""
    return {'results': [{'mode': 'native', 'case': case, 'samples': [{'ns': value * 1000, 'iterations': 1000}]}
                        for case, values in cases.items() for value in values]}


def failures(cases, tolerance=0.02):
    original = gate.PAIRS
    gate.PAIRS = {'test': [('native', 'generated', 'manual', None, '')]}
    try:
        with contextlib.redirect_stdout(io.StringIO()):
            return gate.check(results(cases), 'test', tolerance)
    finally:
        gate.PAIRS = original


class GateTest(unittest.TestCase):
    def test_consistent_regression_fails(self):
        # The November 10 typed critical FFM gap: +15%, every process slower.
        self.assertEqual(failures({'generated': [1.595, 1.598, 1.601, 1.610, 1.646],
                                   'manual': [1.393, 1.393, 1.393, 1.394, 1.397]}), ['native/generated vs manual'])

    def test_parity_passes(self):
        self.assertEqual(failures({'generated': [1.412, 1.413, 1.414, 1.415, 1.416],
                                   'manual': [1.393, 1.393, 1.394, 1.395, 1.397]}), [])

    def test_overlapping_noise_passes(self):
        # A shared host widens both ranges; a higher median alone is not a regression.
        self.assertEqual(failures({'generated': [0.939, 0.990, 1.032, 1.200, 1.366],
                                   'manual': [0.947, 0.960, 1.009, 1.300, 1.680]}), [])

    def test_one_outlier_reference_cannot_hide_a_regression(self):
        self.assertEqual(failures({'generated': [1.60, 1.60, 1.60, 1.60, 1.60],
                                   'manual': [1.39, 1.39, 1.39, 1.39, 9.99]}), ['native/generated vs manual'])

    def test_one_outlier_generated_process_cannot_fail(self):
        self.assertEqual(failures({'generated': [1.39, 1.39, 1.39, 1.39, 9.99],
                                   'manual': [1.39, 1.39, 1.39, 1.39, 1.39]}), [])

    def test_missing_case_fails(self):
        self.assertEqual(failures({'generated': [1.0] * 5}), ['native/generated vs manual'])

    def test_every_pair_names_known_cases(self):
        for suite, pairs in gate.PAIRS.items():
            for mode, generated, reference, allowance, reason in pairs:
                with self.subTest(suite=suite, generated=generated):
                    self.assertNotEqual(generated, reference)
                    self.assertEqual(allowance is None, not reason, 'an allowance must state its reason')


if __name__ == '__main__':
    unittest.main()

// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
/* Bemo's own header, which reaches the C query only through the generated context's delegation. */
#ifndef BEMO_CONTEXT_MACRO
#error "the consumer context's macro definitions did not reach the C query"
#endif
#ifndef BEMO_CONTEXT_OPTION
#error "the consumer context's compiler options did not reach the C query"
#endif
#ifdef BEMO_FIXTURE_SEAM_H
#error "seam.h came before the consumer context's headers"
#endif

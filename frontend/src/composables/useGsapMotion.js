import { onUnmounted, nextTick } from 'vue'
import gsap from 'gsap'

/**
 * GSAP motion composable with automatic cleanup.
 * Usage:
 *   const { ctx, animateIn, hoverIn, hoverOut, breathe } = useGsapMotion(rootRef)
 */
export function useGsapMotion(rootRef) {
  let ctx = null

  function ensureCtx() {
    if (!ctx && rootRef?.value) {
      ctx = gsap.context(() => {}, rootRef.value)
    }
    return ctx
  }

  /**
   * Stagger-reveal a list of elements from below.
   */
  function animateIn(selector, opts = {}) {
    const c = ensureCtx()
    if (!c) return
    const targets = rootRef.value.querySelectorAll(selector)
    if (!targets.length) return
    gsap.from(targets, {
      opacity: 0,
      y: opts.y ?? 24,
      duration: opts.duration ?? 0.7,
      ease: 'power3.out',
      stagger: opts.stagger ?? 0.08,
      delay: opts.delay ?? 0,
    })
  }

  /**
   * Fade + slide a single element in.
   */
  function fadeUp(el, opts = {}) {
    const c = ensureCtx()
    if (!c || !el) return
    gsap.from(el, {
      opacity: 0,
      y: opts.y ?? 20,
      duration: opts.duration ?? 0.6,
      ease: 'power3.out',
      delay: opts.delay ?? 0,
    })
  }

  /**
   * Hover-in animation for cards (lift + subtle scale).
   */
  function hoverIn(el) {
    if (!el) return
    gsap.to(el, {
      y: -6,
      scale: 1.015,
      duration: 0.35,
      ease: 'power3.out',
    })
  }

  /**
   * Hover-out animation for cards (reset).
   */
  function hoverOut(el) {
    if (!el) return
    gsap.to(el, {
      y: 0,
      scale: 1,
      duration: 0.35,
      ease: 'power3.out',
    })
  }

  /**
   * Subtle aurora breathing glow on an element.
   */
  function breathe(el, opts = {}) {
    const c = ensureCtx()
    if (!c || !el) return
    return gsap.to(el, {
      boxShadow: opts.shadow ?? '0 0 40px rgba(16, 185, 129, 0.08)',
      duration: opts.duration ?? 3,
      repeat: -1,
      yoyo: true,
      ease: 'sine.inOut',
    })
  }

  /**
   * Stagger-reveal cards with a slight scale effect.
   */
  function staggerCards(selector, opts = {}) {
    const c = ensureCtx()
    if (!c) return
    const targets = rootRef.value.querySelectorAll(selector)
    if (!targets.length) return
    gsap.from(targets, {
      opacity: 0,
      y: 30,
      scale: 0.96,
      duration: opts.duration ?? 0.65,
      ease: 'power3.out',
      stagger: opts.stagger ?? 0.1,
      delay: opts.delay ?? 0.15,
    })
  }

  /**
   * Animate a ring/glow appearing on selection.
   */
  function selectGlow(el) {
    if (!el) return
    gsap.fromTo(el, {
      boxShadow: '0 0 0 0 rgba(16, 185, 129, 0)',
    }, {
      boxShadow: '0 0 20px rgba(16, 185, 129, 0.12), inset 0 0 0 1px rgba(16, 185, 129, 0.4)',
      duration: 0.3,
      ease: 'power2.out',
    })
  }

  onUnmounted(() => {
    if (ctx) {
      ctx.revert()
      ctx = null
    }
  })

  return {
    ensureCtx,
    animateIn,
    fadeUp,
    hoverIn,
    hoverOut,
    breathe,
    staggerCards,
    selectGlow,
  }
}

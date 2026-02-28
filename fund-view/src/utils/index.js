/**
 * 防抖函数
 */
export function debounce(fn, delay) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 格式化数字
 */
export function formatNumber(num, decimals = 2) {
  if (num === null || num === undefined) return '-'
  return Number(num).toFixed(decimals)
}

/**
 * 格式化百分比
 */
export function formatPercent(num, decimals = 2) {
  if (num === null || num === undefined) return '-'
  const val = Number(num)
  const sign = val > 0 ? '+' : ''
  return `${sign}${val.toFixed(decimals)}%`
}

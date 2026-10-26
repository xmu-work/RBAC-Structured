/**
 * 时间工具（契约 3.3 双格式，时间处理禁止手写字符串拼接）
 * - 响应时间：ISO 本地时间（如 2026-10-24T16:13:19.170295），展示前经 formatDateTime 格式化，只格式化不做时区转换（契约 9.2-6）
 * - 请求时间参数：ISO 本地格式 YYYY-MM-DDTHH:mm:ss（不带时区后缀）
 */
import dayjs, { type Dayjs } from 'dayjs'

/** 展示用：后端 ISO 本地时间 → YYYY-MM-DD HH:mm:ss；空值返回占位符 */
export function formatDateTime(value: string | null | undefined, empty = '—'): string {
  if (!value) {
    return empty
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : value
}

/** el-table-column 的 formatter：直接用于 prop 时间列 */
export function dateTimeColumnFormatter(_row: unknown, _column: unknown, value: string | null | undefined): string {
  return formatDateTime(value)
}

/** 请求参数用：Date/Dayjs/字符串/时间戳 → ISO 本地格式（如审计 startTime/endTime、授权 expireTime） */
export function toLocalISO(input: Date | Dayjs | string | number): string {
  return dayjs(input).format('YYYY-MM-DDTHH:mm:ss')
}

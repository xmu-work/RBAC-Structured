/**
 * 时间工具（契约 3.3 双格式，时间处理禁止手写字符串拼接）
 * - 响应时间：yyyy-MM-dd HH:mm:ss 字符串，原样直显，禁止二次时区转换（契约 9.2-6）
 * - 请求时间参数：ISO 本地格式 YYYY-MM-DDTHH:mm:ss（不带时区后缀）
 */
import dayjs, { type Dayjs } from 'dayjs'

/** 请求参数用：Date/Dayjs/字符串/时间戳 → ISO 本地格式（如审计 startTime/endTime、授权 expireTime） */
export function toLocalISO(input: Date | Dayjs | string | number): string {
  return dayjs(input).format('YYYY-MM-DDTHH:mm:ss')
}

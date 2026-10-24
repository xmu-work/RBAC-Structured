import type { App } from 'vue'
import vPerm from './perm'

export function registerDirectives(app: App): void {
  app.directive('perm', vPerm)
}

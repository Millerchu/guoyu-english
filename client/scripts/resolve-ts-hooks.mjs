const relativeSpecifier = /^\.{1,2}\//;
const hasExtension = /\.[cm]?[jt]s$/;

export async function resolve(specifier, context, nextResolve) {
  if (relativeSpecifier.test(specifier) && !hasExtension.test(specifier)) {
    return nextResolve(`${specifier}.ts`, context);
  }
  return nextResolve(specifier, context);
}

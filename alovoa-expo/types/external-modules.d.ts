declare module "@xenova/transformers" {
  export const pipeline: (...args: any[]) => Promise<any>;
}

declare module "whisper.rn" {
  export const initWhisper: (...args: any[]) => Promise<any>;
  export const transcribe: (...args: any[]) => Promise<any>;
}

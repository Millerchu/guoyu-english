"""生成工具的可预期错误。消息在抛出前去掉密钥。"""


def scrub(text: str, secrets: list[str]) -> str:
    cleaned = text
    for secret in secrets:
        if secret:
            cleaned = cleaned.replace(secret, "[REDACTED]")
    return cleaned


class TtsError(Exception):
    """配置、引擎或输出不满足契约时抛出。"""

    def __init__(self, message: str, secrets: list[str] | None = None) -> None:
        super().__init__(scrub(message, secrets or []))

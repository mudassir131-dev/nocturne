# FAQ: How does InnerTube fetch audio streams without official API keys?

## Question
How does InnerTube fetch audio streams without official API keys?

## Answer
InnerTube is the official internal JSON and Protobuf API used by YouTube and YouTube Music native web and mobile clients. By mimicking official client payloads (such as the Android Music client identity), Nocturne requests stream formats, metadata, and search results directly without needing Google Cloud API quotas or developer keys.

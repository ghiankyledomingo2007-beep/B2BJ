"""Small checks for the native-pixel cleanup; no Blender or editor needed."""
from PIL import Image
from post_pixel import polish_body

image = Image.new("RGBA", (5, 5))
image.putpixel((1, 1), (41, 46, 102, 255))
image.putpixel((2, 2), (224, 48, 64, 255))
image.putpixel((2, 3), (224, 48, 64, 255))
image.putpixel((3, 3), (224, 48, 64, 255))
image.putpixel((4, 1), (243, 222, 188, 255))
before = image.tobytes()
result = polish_body(image)
assert image.tobytes() == before, "never mutate the original cel"
assert result.getchannel("A").tobytes() == image.getchannel("A").tobytes(), "silhouette must stay intact"
assert sum(p == (224, 48, 64, 255) for p in result.get_flattened_data()) == 1
assert result.getpixel((1, 1)) == (44, 51, 113, 255)
assert result.getpixel((4, 1)) == image.getpixel((4, 1)), "skin ramp stays intact"
assert polish_body(result).tobytes() == result.tobytes(), "repeated exports must not keep changing pixels"
assert not polish_body(Image.new("RGBA", (5, 5))).getbbox()
print("PASS: alpha, source preservation, single dot, skin tone and repeatable cleanup")

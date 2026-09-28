import os
import subprocess

svg_content = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1090 1090">
  <defs>
    <linearGradient id="innerGrad" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#6d6e73"/>
      <stop offset="100%" stop-color="#4a4b50"/>
    </linearGradient>
    <clipPath id="roundedClip">
      <path d="M 237 0 H 853 A 237 237 0 0 1 1090 237 V 853 A 237 237 0 0 1 853 1090 H 237 A 237 237 0 0 1 0 853 V 237 A 237 237 0 0 1 237 0 Z"/>
    </clipPath>
  </defs>
  <g clip-path="url(#roundedClip)">
    <path d="M 237 0 H 853 A 237 237 0 0 1 1090 237 V 853 A 237 237 0 0 1 853 1090 H 237 A 237 237 0 0 1 0 853 V 237 A 237 237 0 0 1 237 0 Z" fill="url(#innerGrad)"/>
    <g transform="translate(545.0,545.0) scale(0.9) translate(-545.0,-549.5)">
      <path d="M 617 479 L 609 481 L 600 486 L 590 497 L 585 509 L 584 514 L 584 593 L 589 607 L 595 615 L 601 620 L 613 625 L 627 625 L 633 623 L 641 618 L 649 609 L 654 598 L 656 589 L 656 516 L 651 499 L 648 494 L 639 485 L 628 480 Z M 459 479 L 451 481 L 439 488 L 433 495 L 427 508 L 426 512 L 426 593 L 432 608 L 441 618 L 453 624 L 467 625 L 474 623 L 482 618 L 488 612 L 495 599 L 497 591 L 497 514 L 494 503 L 489 494 L 484 488 L 472 481 Z M 736 138 L 721 137 L 708 141 L 699 142 L 680 147 L 675 147 L 630 156 L 624 156 L 611 159 L 597 160 L 596 161 L 576 163 L 568 165 L 520 170 L 519 171 L 489 175 L 451 185 L 425 195 L 400 207 L 369 225 L 336 248 L 306 273 L 281 298 L 262 320 L 233 361 L 216 391 L 206 414 L 204 416 L 203 421 L 200 426 L 194 445 L 192 448 L 183 481 L 178 507 L 177 519 L 176 520 L 175 542 L 174 543 L 174 596 L 175 597 L 177 625 L 187 673 L 201 716 L 209 735 L 224 765 L 241 793 L 260 819 L 280 842 L 310 871 L 349 901 L 381 920 L 408 933 L 447 947 L 475 954 L 496 957 L 497 958 L 503 958 L 504 959 L 512 959 L 520 961 L 534 961 L 535 962 L 574 962 L 575 961 L 586 961 L 587 960 L 611 958 L 641 952 L 680 940 L 701 931 L 742 909 L 771 889 L 794 870 L 822 842 L 849 808 L 870 775 L 890 735 L 899 709 L 901 706 L 912 666 L 912 661 L 914 656 L 916 635 L 913 619 L 906 606 L 895 595 L 886 590 L 876 587 L 871 587 L 870 586 L 740 586 L 730 588 L 717 595 L 711 600 L 703 611 L 698 626 L 698 641 L 700 649 L 705 659 L 717 671 L 732 678 L 750 679 L 751 680 L 748 688 L 735 710 L 718 732 L 698 752 L 666 776 L 652 784 L 650 784 L 646 787 L 644 787 L 630 794 L 599 803 L 583 806 L 575 806 L 574 807 L 534 807 L 533 806 L 513 804 L 496 800 L 463 788 L 438 775 L 422 764 L 406 751 L 389 734 L 375 717 L 363 699 L 347 668 L 335 634 L 335 630 L 330 611 L 329 596 L 328 595 L 328 550 L 335 513 L 339 503 L 339 500 L 350 473 L 362 451 L 372 436 L 392 412 L 411 394 L 425 383 L 459 363 L 461 363 L 475 356 L 496 349 L 519 344 L 534 343 L 535 342 L 573 342 L 574 343 L 582 343 L 612 349 L 630 355 L 651 364 L 653 366 L 664 371 L 688 387 L 722 417 L 731 423 L 752 432 L 767 435 L 778 435 L 779 434 L 790 433 L 809 425 L 825 412 L 837 395 L 844 377 L 844 372 L 846 365 L 846 352 L 843 337 L 835 318 L 825 303 L 815 291 L 786 263 L 787 260 L 795 255 L 807 243 L 816 230 L 820 218 L 819 206 L 814 197 L 809 193 L 800 190 L 793 190 L 788 192 L 780 193 L 743 205 L 722 210 L 721 208 L 732 197 L 745 178 L 750 163 L 750 156 L 748 149 L 744 143 Z" fill="#ffffff"/>
    </g>
  </g>
</svg>"""

with open("/tmp/app_icon.svg", "w") as f:
    f.write(svg_content)

# Render base high resolution PNG (1024x1024)
subprocess.run(["rsvg-convert", "-w", "1024", "-h", "1024", "/tmp/app_icon.svg", "-o", "/tmp/app_icon_1024.png"], check=True)

densities = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

base_res = "app/src/main/res"

for name, size in densities.items():
    folder = os.path.join(base_res, f"mipmap-{name}")
    os.makedirs(folder, exist_ok=True)
    
    # Remove webp if exists
    for webp in ["ic_launcher.webp", "ic_launcher_round.webp"]:
        webp_path = os.path.join(folder, webp)
        if os.path.exists(webp_path):
            os.remove(webp_path)
            
    # Generate ic_launcher.png (square / squircle as in SVG)
    launcher_png = os.path.join(folder, "ic_launcher.png")
    subprocess.run(["convert", "/tmp/app_icon_1024.png", "-resize", f"{size}x{size}!", f"PNG32:{launcher_png}"], check=True)
    
    # Generate ic_launcher_round.png (masked to circle)
    launcher_round_png = os.path.join(folder, "ic_launcher_round.png")
    radius = size / 2
    cmd = [
        "convert", "/tmp/app_icon_1024.png", "-resize", f"{size}x{size}!",
        "(", "-size", f"{size}x{size}", "xc:none", "-fill", "white",
        "-draw", f"circle {radius},{radius} {radius},0", ")",
        "-alpha", "set", "-compose", "DstIn", "-composite", f"PNG32:{launcher_round_png}"
    ]
    subprocess.run(cmd, check=True)

print("Icons generated successfully!")

from PIL import Image, ImageChops
import numpy as np
import sys

def Standard_Deviation(img1, img2):
    # 计算每个像素的（个体的）标准差
    std_list = []
    # 遍历图片的每个像素  
    for x in range(img1.width):
        for y in range(img1.height):
            # 获取对应位置的像素值  
            pixel1 = img1.getpixel((x, y))
            pixel2 = img2.getpixel((x, y))

            # 计算个体标准差
            std_list.append(np.std([pixel1, pixel2]))

    return np.round(np.mean(std_list), 2), np.round(np.max(std_list), 2), np.round(np.min(std_list), 2)


def Standard_Deviation(img1, img2):
    # 计算每个像素的（个体的）标准差
    img1 = np.array(img1)
    img2 = np.array(img2)
    std_dev = np.abs(img1 - img2) / 2.0
    mean_std = std_dev.mean()
    max_std = std_dev.max()
    min_std = std_dev.min()
    return np.around(mean_std, 2), np.around(max_std, 2), np.around(min_std, 2)

def Image_Diff_Cal(lastOnePath, lastSecondPath):
    lastOneImg = Image.open(lastOnePath)
    lastSecondImg = Image.open(lastSecondPath)

    # 尺寸一致检测
    if lastOneImg.size != lastSecondImg.size:
        err = f'图片尺寸不一致,last One:{lastOneImg.size}, last second:{lastSecondImg.size}'
        raise ValueError(err)

    # 计算所有位置的标准差
    mean_std, max_std, min_std = Standard_Deviation(lastOneImg, lastSecondImg)

    return mean_std, max_std, min_std


if __name__ == '__main__':
    lastOnePath = sys.argv[1]
    lastSecondPath = sys.argv[2]
    try:
        mean_std, max_std, min_std = Image_Diff_Cal(lastOnePath, lastSecondPath)
        print(f'check blank success:[{mean_std},{max_std},{min_std}]')
    except Exception as e:
        print(e)
        print("check blank failed")

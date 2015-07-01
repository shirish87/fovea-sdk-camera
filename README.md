# fovea-sdk-camera

> Android SDK 14+.

Reduces the tedious and complex job of creating a custom Camera and capturing a picture to the following interface:

```
public class CameraActivity extends FoveaCameraActivity {

    @Override
    public int provideLayout() {
        // must contain certain basic components
        // but layout and elements can be customized
        return R.layout.fragment_camera;
    }
    
    @Override
    public void onFragmentViewCreated(View view) {
        // attach additional event listeners, etc.
    }
    
    @Override
    public void onPictureTaken(String path) {
        Toast.makeText(this, "Saved at: " + path, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(Throwable e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    }
}
```

**Note**: This is a work in progress that may see breaking changes!



# License

Images included in this project are protected by their respective licenses.

Copyright (c) 2015 Shirish Kamath.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

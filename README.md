NitroXy Wowza Module
===================

NitroXy Media's module for Wowza.

* Stream switching
* Remote control via json (and website for doing that)

Installation
------------

1. Download jar or build module yourself (see build instructions below)
2. Put jar in `/usr/local/WowzaStreamingEngine/lib`.
3. In `/usr/local/WowzaStreamingEngine/conf/Server.xml` add a `ServerListener`: 

        <ServerListener>
          <BaseClass>com.nitroxy.wmz.module.ServerListener</BaseClass>
        </ServerListener>

4. In `/usr/local/WowzaStreamingEngine/conf/VHost.xml` add a `HTTPProvider` in the Admin section: 

        <HTTPProvider>
          <BaseClass>com.nitroxy.wmz.module.RESTProvider</BaseClass>
          <RequestFilters>nitroxy*</RequestFilters>
          <AuthenticationMethod>none</AuthenticationMethod>
        </HTTPProvider>

5. In `/usr/local/WowzaStreamingEngine/conf/$application/Application.xml` add the module: 

        <Module>
          <Name>NitroXyModule</Name>
          <Description>NitroXy stream control module</Description>
          <Class>com.nitroxy.wmz.module.NitroXyModule</Class>
        </Module>

6. Configure webserver to serve the content under `wowza_nx_control` and to proxy `/api` to `localhost:8086/nitroxy`
See `ansible/ngigx.conf` for an example of how this can be setup.

Building module
---------------

### Using Vagrant

The recommended and easiest way is to use Vagrant.

1. `vagrant up`
2. `ant`
3. (Optional) If you need to test changes you need to restart the service: 
`vagrant ssh` followed by `sudo service WowzaStreamingEngine restart`

### Wowza IDE

1. Install Wowza.
2. Install Eclipse and [Wowza IDE](http://www.wowza.com/streaming/developers/wowza-ide-software-update)
3. Put the repo in a folder called NitroxyWowzaModule (case sensitive due to the way wowza builds modules)
4. Add a new "Wowza Media Server Java Project" with these settings: 
Project name: NitroxyWowzaModule  
Wowza location: /usr/local/WowzaStreamingEngine  
Package: com.nitroxy.wmz.module  
Name: NitroXyModule
5. Also ensure the java compatibility level is at least 1.6 (it is stored both globally for eclipse and per project)

Version history
---------------

### v1.1

REST API is now enabled by adding a HTTPProvider under 'VHost.xml`:

    <HTTPProvider>
      <BaseClass>com.nitroxy.wmz.module.RESTProvider</BaseClass>
      <RequestFilters>nitroxy*</RequestFilters>
      <AuthenticationMethod>none</AuthenticationMethod>
    </HTTPProvider>

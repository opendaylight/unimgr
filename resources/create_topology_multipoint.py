#!/usr/bin/python

"""
This script creates topology to experiment with ovs drivers.
"""

import re
import sys
import os
import time

from mininet.cli import CLI
from mininet.log import setLogLevel, info, error
from mininet.net import Mininet
from mininet.link import Intf
from mininet.util import quietRun
from mininet.node import Controller, RemoteController
from mininet.topo import Topo
from mininet.node import Node
from mininet.util import waitListening
from functools import partial
from mininet.node import OVSSwitch

class PocTopo( Topo ):
	"Topology prepared for Presto NRP tutorial"

	def __init__( self ):

        # Initialize topology
		Topo.__init__( self )

		# Add hosts and switches
		h1 = self.addHost( 'h1' )
		h2 = self.addHost( 'h2' )
                h3 = self.addHost( 'h3' )

		s1 = self.addSwitch( 's1' )
		s2 = self.addSwitch( 's2' )
		s3 = self.addSwitch( 's3' )
                s4 = self.addSwitch( 's4' )
        # Add links
		self.addLink( h1, s1 )
		self.addLink( h2, s2 )
		self.addLink( h3, s3 )
		self.addLink( s1, s4 )
                self.addLink( s2, s4 )
                self.addLink( s3, s4 )


#topos = { 'poctopo': ( lambda: PocTopo() ) }

if __name__ == '__main__':
	setLogLevel( 'info' )

	os.system('ovs-vsctl set-manager ptcp:6640')

	defaultIF1 = 'enp0s3'
	defaultIF2 = 'enp0s8'
        defaultIF3 = 'enp0s9'
    #    defaultIF4 = 'enp0s10'
	defaultControllerIP = '127.0.0.1'
	defaultInputSwitch = 0
	defaultOutputSwitch = 1

    # try to get hw intfs from the command line; by default, use eth1 and eth2
	odl_controller_ip = sys.argv[ 1 ] if len( sys.argv ) > 1 else defaultControllerIP
	intfName = sys.argv[ 2 ] if len( sys.argv ) > 2 else ""
	intfName2 = sys.argv[ 3 ] if len( sys.argv ) > 3 else ""
        intfName3 = sys.argv[ 4 ] if len( sys.argv ) > 4 else ""
        intfName4 = sys.argv[ 5 ] if len( sys.argv ) > 5 else ""
	input_switch0 = 0
	input_switch1 = 1
        input_switch2 = 2
     #   input_switch3 = 3

	OVSSwitch13 = partial( OVSSwitch, protocols='OpenFlow13' )

	topo = PocTopo( )

	net = Mininet(topo, switch=OVSSwitch13, controller=partial( RemoteController, ip=odl_controller_ip, port=6633 ) )

	if intfName != "":
		switch = net.switches[ input_switch0 ]
		info( '*** Adding hardware interface', intfName, 'to switch', switch.name, '\n' )
		Intf( intfName, node=switch )

	if intfName2 != "":
		switch2 = net.switches[ input_switch1 ]
		info( '*** Adding hardware interface', intfName2, 'to switch', switch2.name, '\n' )
		Intf(intfName2, node=switch2)

        if intfName3 != "":
                switch3 = net.switches[ input_switch2 ]
                info( '*** Adding hardware interface', intfName3, 'to switch', switch3.name, '\n' )
                Intf(intfName3, node=switch3)
        

	net.start()

#	os.system('ovs-ofctl -O OpenFlow13 del-flows s'+str(input_switch+1))
#	os.system('ovs-ofctl -O OpenFlow13 del-flows s'+str(output_switch+1))
	CLI( net )
	net.stop()
